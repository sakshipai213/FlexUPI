package com.fintech.upisimulator.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class StandardUPIFeeStrategy implements FeeCalculationStrategy {
    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        // Standard Credit on UPI baseline: 0% transactional fee
        return BigDecimal.ZERO;
    }
}