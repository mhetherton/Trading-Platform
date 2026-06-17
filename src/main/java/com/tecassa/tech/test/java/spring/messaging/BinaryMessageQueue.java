package com.tecassa.tech.test.java.spring.messaging;

import com.tecassa.tech.test.java.spring.domain.Order;
import com.tecassa.tech.test.java.spring.matching.OrderMatcher;
import com.tecassa.tech.test.java.spring.serdes.Serdes;
import org.springframework.stereotype.Component;

/**
 * Do not change this class.
 */
@Component
public class BinaryMessageQueue {

    private final Serdes<Order> serdes;

    private final OrderMatcher orderMatcher;

    public BinaryMessageQueue(Serdes<Order> serdes, OrderMatcher orderMatcher) {
        this.serdes = serdes;
        this.orderMatcher = orderMatcher;
    }

    public void publish(Order order){
        //This is just to simulate serialisation over some ipc or message barrier
        byte[] bytes = serdes.serialise(order);
        Order rehydrated = serdes.deserialise(bytes);
        orderMatcher.newOrder(rehydrated);
    }
}
