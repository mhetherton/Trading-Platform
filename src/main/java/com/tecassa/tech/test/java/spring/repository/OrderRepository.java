package com.tecassa.tech.test.java.spring.repository;

import com.tecassa.tech.test.java.spring.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find active SELLS for a BUY order (Lowest price first)
    @Query("SELECT o FROM Order o WHERE o.side = 'SELL' AND o.status = 'ACTIVE' ORDER BY o.price ASC")
    List<Order> findMatchingSells();

    // Find active BUYS for a SELL order (Highest price first)
    @Query("SELECT o FROM Order o WHERE o.side = 'BUY' AND o.volume > 0 ORDER BY o.price DESC")
    List<Order> findMatchingBuys();
}