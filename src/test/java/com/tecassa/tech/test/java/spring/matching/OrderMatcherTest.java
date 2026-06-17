package com.tecassa.tech.test.java.spring.matching;

import com.tecassa.tech.test.java.spring.domain.Order;
import com.tecassa.tech.test.java.spring.domain.Order.Side;
import com.tecassa.tech.test.java.spring.domain.Order.OrderStatus;
import com.tecassa.tech.test.java.spring.domain.Trade;
import com.tecassa.tech.test.java.spring.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Fill out some tests to show how you would go about it.
 * No need to overdo the number of tests but feel free to
 * list out some tests you would continue to write if you had
 * more time and wanted to fully surround the test cases.
 */
@SpringBootTest
@Transactional
class OrderMatcherTest {

    @Autowired
    private OrderMatcher orderMatcher;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private TradePublisher tradePublisher;

    @BeforeEach
    public void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    public void twoMatchingOrdersProduceTrade() {
        // 1. Arrange: Place a resting SELL order on the book
        Order restingSell = Order.builder()
                .side(Side.SELL)
                .price(new BigDecimal("100.00"))
                .volume(50L)
                .status(OrderStatus.ACTIVE)
                .build();
        orderRepository.saveAndFlush(restingSell);

        // 2. Act: Submit a matching incoming BUY order (Price >= Sell Price)
        Order incomingBuy = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("100.00"))
                .volume(50L)
                .build();

        orderMatcher.newOrder(incomingBuy);

        // 3. Assert: Verify a trade was generated and published
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradePublisher, times(1)).publishTrade(tradeCaptor.capture());

        Trade publishedTrade = tradeCaptor.getValue();
        assertEquals(50L, publishedTrade.getVolume());
        assertEquals(new BigDecimal("100.00"), publishedTrade.getPrice());

        // Verify DB states: Both orders should now be completely FILLED with 0 volume
        List<Order> allOrders = orderRepository.findAll();
        assertEquals(2, allOrders.size());
        for (Order order : allOrders) {
            assertEquals(0L, order.getVolume());
            assertEquals(OrderStatus.FILLED, order.getStatus());
        }
    }

    @Test
    public void twoNonMatchingOrdersDoNotProduceTrade() {
        // 1. Arrange: Place a resting SELL order at a high price
        Order restingSell = Order.builder()
                .side(Side.SELL)
                .price(new BigDecimal("150.00"))
                .volume(50L)
                .status(OrderStatus.ACTIVE)
                .build();
        orderRepository.saveAndFlush(restingSell);

        // 2. Act: Submit an incoming BUY order at a lower price (No price cross)
        Order incomingBuy = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("100.00"))
                .volume(50L)
                .build();

        orderMatcher.newOrder(incomingBuy);

        // 3. Assert: Verify no trades were published
        verify(tradePublisher, never()).publishTrade(any(Trade.class));

        // Verify DB states: Both orders must remain ACTIVE with their full original
        // volumes
        List<Order> activeOrders = orderRepository.findAll();
        assertEquals(2, activeOrders.size());
        for (Order order : activeOrders) {
            assertEquals(50L, order.getVolume());
            assertEquals(OrderStatus.ACTIVE, order.getStatus());
        }
    }

    /*
     * A Buy order for 100 volume matches a Sell order for 40 volume. Verify
     * one trade of 40 is published, and the Buy order correctly remains in the
     * memory book with 60 volume remaining.
     */
    @Test
    void twoNonMatchingOrdersValidateVolumneRemaining() {
        // 1. Arrange: Create and save a resting Sell order with 40 volume
        Order restingSell = Order.builder()
                .side(Side.SELL)
                .price(new BigDecimal("100.00"))
                .volume(40L)
                .status(OrderStatus.ACTIVE)
                .build();
        orderRepository.saveAndFlush(restingSell);

        // 2. Act: Incoming large Buy order of 100 volume
        Order incomingBuy = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("100.00"))
                .volume(100L)
                .build();

        orderMatcher.newOrder(incomingBuy);

        // 3. Assert: Verify exactly 1 trade of 40 volume was published
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradePublisher, times(1)).publishTrade(tradeCaptor.capture());

        Trade trade = tradeCaptor.getValue();
        assertEquals(40L, trade.getVolume());
        assertEquals(new BigDecimal("100.00"), trade.getPrice());

        // Verify resting Sell is fully FILLED (0 volume left)
        Order updatedSell = orderRepository.findById(restingSell.getOrderId()).orElseThrow();
        assertEquals(0L, updatedSell.getVolume());
        assertEquals(OrderStatus.FILLED, updatedSell.getStatus());

        // Verify incoming Buy order stays ACTIVE in the book with exactly 60 volume
        // remaining
        Order updatedBuy = orderRepository.findById(incomingBuy.getOrderId()).orElseThrow();
        assertEquals(60L, updatedBuy.getVolume());
        assertEquals(OrderStatus.ACTIVE, updatedBuy.getStatus());
    }

    /*
     * A large Buy order of 100 arrives. The book has three resting Sell orders of
     * 30, 30, and 40 at the same price. Verify the TradePublisher is called exactly
     * 3 times with distinct trades.
     */
    @Test
    void threeSellOrdersValidateOrderProducesThreeTrade() {
        // 1. Arrange: Setup 3 resting Sell orders on the book at the same price
        Order sell1 = Order.builder().side(Side.SELL).price(new BigDecimal("100.00")).volume(30L)
                .status(OrderStatus.ACTIVE).build();
        Order sell2 = Order.builder().side(Side.SELL).price(new BigDecimal("100.00")).volume(30L)
                .status(OrderStatus.ACTIVE).build();
        Order sell3 = Order.builder().side(Side.SELL).price(new BigDecimal("100.00")).volume(40L)
                .status(OrderStatus.ACTIVE).build();

        orderRepository.saveAllAndFlush(List.of(sell1, sell2, sell3));

        // 2. Act: Large incoming Buy order arrives sweeping the entire book volume (30
        // + 30 + 40 = 100)
        Order incomingBuy = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("100.00"))
                .volume(100L)
                .build();

        orderMatcher.newOrder(incomingBuy);

        // 3. Assert: Verify the publisher was triggered exactly 3 times
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradePublisher, times(3)).publishTrade(tradeCaptor.capture());

        List<Trade> publishedTrades = tradeCaptor.getAllValues();

        // Assert the sizes/volumes match the expectations sequentially
        assertEquals(30L, publishedTrades.get(0).getVolume());
        assertEquals(30L, publishedTrades.get(1).getVolume());
        assertEquals(40L, publishedTrades.get(2).getVolume());

        // Verify that the incoming Buy swept everything, leaving all 4 orders FILLED
        // with 0 volume
        List<Order> entireBook = orderRepository.findAll();
        assertEquals(4, entireBook.size());
        for (Order order : entireBook) {
            assertEquals(0L, order.getVolume(), "Order " + order.getOrderId() + " should have 0 volume.");
            assertEquals(OrderStatus.FILLED, order.getStatus(), "Order " + order.getOrderId() + " should be FILLED.");
        }
    }

    /*
     * A Buy order arrives at $110.00. A resting Sell order sits at $100.00.
     * Verify they match, and the resulting trade executes at the resting
     * order's price ($100.00), not the incoming price.
     */
    @Test
    void incomingOrderReceivesPriceImprovement() {
        Order restingSell = Order.builder()
                .side(Side.SELL)
                .price(new BigDecimal("100.00"))
                .volume(10L)
                .status(OrderStatus.ACTIVE)
                .build();
        orderRepository.saveAndFlush(restingSell);

        Order incomingBuy = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("110.00")) // Higher aggressive buy price
                .volume(10L)
                .build();

        orderMatcher.newOrder(incomingBuy);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradePublisher, times(1)).publishTrade(tradeCaptor.capture());

        Trade trade = tradeCaptor.getValue();
        assertEquals(new BigDecimal("100.00"), trade.getPrice(), "Trade must execute at the resting limit price.");
    }

    /*
     * An incoming Buy order of 50 volume arrives.
     * The book has two resting Sells of 20 volume each.
     * Verify 2 trades of 20 are published, and the Buy order remains ACTIVE with 10
     * volume.
     */
    @Test
    void incomingOrderPartiallySweepsMultipleOrders() {
        Order sell1 = Order.builder().side(Side.SELL).price(new BigDecimal("100.00")).volume(20L)
                .status(OrderStatus.ACTIVE).build();
        Order sell2 = Order.builder().side(Side.SELL).price(new BigDecimal("100.00")).volume(20L)
                .status(OrderStatus.ACTIVE).build();
        orderRepository.saveAllAndFlush(List.of(sell1, sell2));

        Order incomingBuy = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("100.00"))
                .volume(50L) // Sweeps both 20s, leaves 10 behind
                .build();

        orderMatcher.newOrder(incomingBuy);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradePublisher, times(2)).publishTrade(tradeCaptor.capture());

        List<Trade> trades = tradeCaptor.getAllValues();
        assertEquals(20L, trades.get(0).getVolume());
        assertEquals(20L, trades.get(1).getVolume());

        // The incoming buy must remain ACTIVE because it still holds 10 volume
        // unexecuted
        Order updatedBuy = orderRepository.findById(incomingBuy.getOrderId()).orElseThrow();
        assertEquals(10L, updatedBuy.getVolume());
        assertEquals(OrderStatus.ACTIVE, updatedBuy.getStatus());
    }

    /*
     * A Buy order arrives at $99.99. A resting Sell sits at $100.00.
     * Verify that even a one-cent difference prevents a trade execution.
     */
    @Test
    void strictPriceBoundaryPreventsExecution() {
        Order restingSell = Order.builder()
                .side(Side.SELL)
                .price(new BigDecimal("100.00"))
                .volume(10L)
                .status(OrderStatus.ACTIVE)
                .build();
        orderRepository.save(restingSell);

        Order incomingBuy = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("99.99")) // Just under the sell price
                .volume(10L)
                .build();

        orderMatcher.newOrder(incomingBuy);

        // Verify zero interactions with the trade publisher
        verify(tradePublisher, never()).publishTrade(any(Trade.class));

        // Verify both orders remain safely ACTIVE on the book
        Order updatedSell = orderRepository.findById(restingSell.getOrderId()).orElseThrow();
        Order updatedBuy = orderRepository.findById(incomingBuy.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.ACTIVE, updatedSell.getStatus());
        assertEquals(OrderStatus.ACTIVE, updatedBuy.getStatus());
    }
}