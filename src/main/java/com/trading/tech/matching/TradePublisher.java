package com.trading.tech.matching;

import com.trading.tech.domain.Trade;
import com.trading.tech.repository.TradeRepository; // Import your repository
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Modified to persist trades directly to the database before publishing.
 */
@Slf4j
@Component
public class TradePublisher {

    private final TradeRepository tradeRepository;

    // Spring will automatically inject the TradeRepository bean here
    public TradePublisher(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public void publishTrade(Trade trade) {
        // 1. Save the trade to the database
        Trade savedTrade = tradeRepository.save(trade);
        log.info("Saved trade to database with ID: {}", savedTrade.getId());

        // 2. Signifies the boundary of the application and provides mock/test
        // termination point
        log.info("Publishing new trade: {}", savedTrade);
    }
}