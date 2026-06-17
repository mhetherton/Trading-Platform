package com.trading.tech.api;

import com.trading.tech.domain.Order;
import com.trading.tech.messaging.BinaryMessageQueue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderApi {

    private final BinaryMessageQueue messageQueue;

    public OrderApi(BinaryMessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody Order order) {
        messageQueue.publish(order);
        return ResponseEntity.ok("");
    }
}
