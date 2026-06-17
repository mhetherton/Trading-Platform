package com.tecassa.tech.test.java.spring.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buyerOrderId; // Changed from String to Long
    private Long sellerOrderId; // Changed from String to Long
    private Long volume;
    private BigDecimal price;

    public Trade(Long buyerOrderId, Long sellerOrderId, Long volume, BigDecimal price) {
        this.buyerOrderId = buyerOrderId;
        this.sellerOrderId = sellerOrderId;
        this.volume = volume;
        this.price = price;
    }
}