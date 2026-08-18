package com.fintech.upisimulator.strategy;

import java.math.BigDecimal;

public interface FeeCalculationStrategy {
    BigDecimal calculateFee(BigDecimal amount);
}