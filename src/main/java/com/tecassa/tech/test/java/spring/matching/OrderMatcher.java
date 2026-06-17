package com.tecassa.tech.test.java.spring.matching;

import com.tecassa.tech.test.java.spring.domain.Order;
import com.tecassa.tech.test.java.spring.domain.Trade;
import com.tecassa.tech.test.java.spring.domain.Order.OrderStatus;
import com.tecassa.tech.test.java.spring.domain.Order.Side;
import com.tecassa.tech.test.java.spring.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class OrderMatcher {

    private final OrderRepository orderRepository;
    private final TradePublisher tradePublisher;

    public OrderMatcher(OrderRepository orderRepository, TradePublisher tradePublisher) {
        this.orderRepository = orderRepository;
        this.tradePublisher = tradePublisher;
    }

    @Transactional
    public synchronized void newOrder(Order incomingOrder) {
        log.info("New order request received. Side: {}, Price: {}, Volume: {}",
                incomingOrder.getSide(), incomingOrder.getPrice(), incomingOrder.getVolume());

        // 2. IMPORTANT: Save it FIRST as ACTIVE to generate the database ID
        // immediately. This ensures savedIncoming.getOrderId() is NOT null inside the
        // loop.
        incomingOrder.setStatus(OrderStatus.ACTIVE);
        Order savedIncoming = orderRepository.save(incomingOrder);

        // 3. Fetch appropriate resting orders based on side
        // Exclude the incoming order's ID so it doesn't match against itself
        List<Order> opposites = (savedIncoming.getSide() == Side.BUY)
                ? orderRepository.findMatchingSells()
                : orderRepository.findMatchingBuys();

        boolean isBuy = savedIncoming.getSide() == Side.BUY;

        // 4. Match against the retrieved book
        for (Order opposite : opposites) {
            if (savedIncoming.getVolume() <= 0) {
                break;
            }

            // Price Cross Check (Buy price must be >= Sell price)
            if (isBuy && savedIncoming.getPrice().compareTo(opposite.getPrice()) < 0)
                break;
            if (!isBuy && savedIncoming.getPrice().compareTo(opposite.getPrice()) > 0)
                break;

            // Determine execution matching volume
            Long tradeVolume = Math.min(savedIncoming.getVolume(), opposite.getVolume());

            // Update volumes on the managed database entities
            savedIncoming.setVolume(savedIncoming.getVolume() - tradeVolume);
            opposite.setVolume(opposite.getVolume() - tradeVolume);

            // Update resting order status cleanly if it's wiped out
            if (opposite.getVolume() == 0) {
                opposite.setStatus(OrderStatus.FILLED);
            }

            // Save the resting order changes cleanly (no mid-loop incoming saves to
            // conflict)
            orderRepository.save(opposite);

            Long buyerId = isBuy ? savedIncoming.getOrderId() : opposite.getOrderId();
            Long sellerId = isBuy ? opposite.getOrderId() : savedIncoming.getOrderId();

            // Construct and submit the trade details
            Trade trade = new Trade(buyerId, sellerId, tradeVolume, opposite.getPrice());
            publishTrade(trade);
        }

        // 5. Final state evaluation for the incoming order
        if (savedIncoming.getVolume() == 0) {
            // Your custom rule: Any match forces the incoming order to FILLED
            savedIncoming.setStatus(OrderStatus.FILLED);
            log.info("Order {} matched. Finalizing status as FILLED in DB.", savedIncoming.getOrderId());
        } else {
            // No trades occurred; stays on the book as available liquidity
            savedIncoming.setStatus(OrderStatus.ACTIVE);
            log.info("Order {} matched nothing. Storing as ACTIVE liquidity.", savedIncoming.getOrderId());
        }

        // Save the final state of the incoming order
        orderRepository.save(savedIncoming);
    }

    private void publishTrade(Trade trade) {
        tradePublisher.publishTrade(trade);
    }
}