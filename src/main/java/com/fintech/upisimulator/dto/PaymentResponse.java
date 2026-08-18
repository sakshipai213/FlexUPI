package com.fintech.upisimulator.dto;

public record PaymentResponse(
        Long transactionId,
        String status,
        String message
) {}