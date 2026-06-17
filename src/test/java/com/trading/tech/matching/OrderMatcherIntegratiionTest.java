package com.trading.tech.matching;

import com.trading.tech.domain.*;
import com.trading.tech.domain.Order.*;
import com.trading.tech.repository.OrderRepository;
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
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class OrderMatcherIntegratiionTest {

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

    /*
     * Two resting Buy orders of 100 volume each are on the book.
     * A large incoming Sell order of 250 volume arrives.
     * Verify exactly 2 trades of 100 are published, the two Buy orders are FILLED,
     * and the remaining 50 volume of the Sell order stays ACTIVE in the database.
     */
    @Test
    void twoRestingBuysSweptByLargeIncomingSell() {
        // 1. Arrange: Place two resting BUY orders on the book at the same price
        Order restingBuy1 = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("100.00"))
                .volume(100L)
                .status(OrderStatus.ACTIVE)
                .build();

        Order restingBuy2 = Order.builder()
                .side(Side.BUY)
                .price(new BigDecimal("100.00"))
                .volume(100L)
                .status(OrderStatus.ACTIVE)
                .build();

        orderRepository.saveAll(List.of(restingBuy1, restingBuy2));

        // 2. Act: Submit a large incoming SELL order of 250 volume
        Order incomingSell = Order.builder()
                .side(Side.SELL)
                .price(new BigDecimal("100.00"))
                .volume(250L)
                .build();

        orderMatcher.newOrder(incomingSell);

        // 3. Assert: Verify exactly 2 trades were published
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradePublisher, times(2)).publishTrade(tradeCaptor.capture());

        List<Trade> publishedTrades = tradeCaptor.getAllValues();
        assertEquals(2, publishedTrades.size());
        assertEquals(100L, publishedTrades.get(0).getVolume());
        assertEquals(100L, publishedTrades.get(1).getVolume());

        // Verify the two resting Buy orders are completely FILLED with 0 volume
        Order updatedBuy1 = orderRepository.findById(restingBuy1.getOrderId()).orElseThrow();
        Order updatedBuy2 = orderRepository.findById(restingBuy2.getOrderId()).orElseThrow();

        assertEquals(0L, updatedBuy1.getVolume());
        assertEquals(OrderStatus.FILLED, updatedBuy1.getStatus());
        assertEquals(0L, updatedBuy2.getVolume());
        assertEquals(OrderStatus.FILLED, updatedBuy2.getStatus());

        // Verify the incoming Sell order has exactly 50 volume remaining and is ACTIVE
        Order updatedSell = orderRepository.findById(incomingSell.getOrderId()).orElseThrow();
        assertEquals(50L, updatedSell.getVolume());
        assertEquals(Side.SELL, updatedSell.getSide());
        assertEquals(OrderStatus.ACTIVE, updatedSell.getStatus());
    }
}
