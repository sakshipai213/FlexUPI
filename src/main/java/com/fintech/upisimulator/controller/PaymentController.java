package com.fintech.upisimulator.controller;

import java.util.List;

import com.fintech.upisimulator.dto.PaymentRequest;
import com.fintech.upisimulator.dto.PaymentResponse;
import com.fintech.upisimulator.dto.RepaymentRequest;
import com.fintech.upisimulator.model.Transaction;
import com.fintech.upisimulator.service.CreditPaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreditPaymentService paymentService;

    public PaymentController(CreditPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/credit-upi")
    public ResponseEntity<PaymentResponse> executeCreditPayment(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.processPayment(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/repay")
    public ResponseEntity<PaymentResponse> repayCredit(@RequestBody RepaymentRequest request) {
        PaymentResponse response = paymentService.processRepayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{vpa}")
    public ResponseEntity<List<Transaction>> getHistory(@PathVariable String vpa) {
        List<Transaction> history = paymentService.getTransactionHistory(vpa);
        return ResponseEntity.ok(history);
    }
}