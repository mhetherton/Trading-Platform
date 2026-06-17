package com.tecassa.tech.test.java.spring.api;

import com.tecassa.tech.test.java.spring.domain.Order;
import com.tecassa.tech.test.java.spring.messaging.BinaryMessageQueue;
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
