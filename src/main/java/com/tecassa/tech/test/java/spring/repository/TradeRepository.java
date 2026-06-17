package com.tecassa.tech.test.java.spring.repository;

import com.tecassa.tech.test.java.spring.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

}