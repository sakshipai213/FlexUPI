package com.fintech.upisimulator.dto;

import java.math.BigDecimal;

public class RepaymentRequest {
    private String vpa;
    private BigDecimal amount;

    // Default constructor
    public RepaymentRequest() {}

    public RepaymentRequest(String vpa, BigDecimal amount) {
        this.vpa = vpa;
        this.amount = amount;
    }

    // Getters and Setters
    public String getVpa() { return vpa; }
    public void setVpa(String vpa) { this.vpa = vpa; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}