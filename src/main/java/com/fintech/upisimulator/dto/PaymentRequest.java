package com.fintech.upisimulator.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        String senderVpa,
        String receiverVpa,
        BigDecimal amount
) {}