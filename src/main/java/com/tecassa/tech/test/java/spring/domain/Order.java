package com.tecassa.tech.test.java.spring.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "orders") // 'order' is a reserved SQL keyword
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    public enum Side {
        BUY, SELL
    }

    public enum OrderStatus {
        ACTIVE, FILLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tells DB to auto-increment this column
    private Long orderId; // Changed from String to Long

    @Enumerated(EnumType.STRING)
    private Side side;

    private BigDecimal price;
    private Long volume;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
}