package com.fintech.upisimulator.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.fintech.upisimulator.dto.PaymentRequest;
import com.fintech.upisimulator.dto.PaymentResponse;
import com.fintech.upisimulator.dto.RepaymentRequest;
import com.fintech.upisimulator.exception.DuplicateRequestException;
import com.fintech.upisimulator.exception.InsufficientCreditException;
import com.fintech.upisimulator.model.*;
import com.fintech.upisimulator.repository.*;
import com.fintech.upisimulator.strategy.FeeCalculationStrategy;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditPaymentService {

    private final UserAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final StringRedisTemplate redisTemplate;
    private final FeeCalculationStrategy feeStrategy;

    public CreditPaymentService(UserAccountRepository accountRepository,
                                TransactionRepository transactionRepository,
                                LedgerEntryRepository ledgerRepository,
                                StringRedisTemplate redisTemplate,
                                FeeCalculationStrategy feeStrategy) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.redisTemplate = redisTemplate;
        this.feeStrategy = feeStrategy;
    }

    @Transactional
    public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
        // 1. Redis Lock / Idempotency Check
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent("lock:" + idempotencyKey, "LOCKED", Duration.ofSeconds(60));

        if (Boolean.FALSE.equals(acquired)) {
            throw new DuplicateRequestException("Transaction with key " + idempotencyKey + " is currently processing or already executed.");
        }

        try {
            // 2. Fetch User Credit Account
            UserAccount account = accountRepository.findByVpa(request.senderVpa())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Sender VPA: " + request.senderVpa()));

            BigDecimal totalDeduction = request.amount().add(feeStrategy.calculateFee(request.amount()));

            if (account.getAvailableCredit().compareTo(totalDeduction) < 0) {
                throw new InsufficientCreditException("Insufficient credit limit available");
            }

            // 3. Update Available Credit
            account.setAvailableCredit(account.getAvailableCredit().subtract(totalDeduction));
            accountRepository.save(account);

            // 4. Create Transaction Record
            Transaction tx = Transaction.builder()
                    .idempotencyKey(idempotencyKey)
                    .senderVpa(request.senderVpa())
                    .receiverVpa(request.receiverVpa())
                    .amount(request.amount())
                    .status(TransactionStatus.SETTLED)
                    .timestamp(LocalDateTime.now())
                    .build();
            transactionRepository.save(tx);

            // 5. Create Double-Entry Ledger Entries
            // Entry A: DEBIT User's Credit Line
            LedgerEntry debitEntry = LedgerEntry.builder()
                    .transactionId(tx.getId())
                    .accountIdentifier(request.senderVpa())
                    .entryType(EntryType.DEBIT)
                    .amount(totalDeduction)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Entry B: CREDIT Merchant/Receiver Account
            LedgerEntry creditEntry = LedgerEntry.builder()
                    .transactionId(tx.getId())
                    .accountIdentifier(request.receiverVpa())
                    .entryType(EntryType.CREDIT)
                    .amount(request.amount())
                    .timestamp(LocalDateTime.now())
                    .build();

            ledgerRepository.save(debitEntry);
            ledgerRepository.save(creditEntry);

            return new PaymentResponse(tx.getId(), tx.getStatus().name(), "Transaction Successful");

        } catch (Exception e) {
            // Release lock if execution failed unexpectedly
            redisTemplate.delete("lock:" + idempotencyKey);
            throw e;
        }
    }

    @Transactional
    public PaymentResponse processRepayment(RepaymentRequest request) {
        UserAccount account = accountRepository.findByVpa(request.getVpa())
                .orElseThrow(() -> new IllegalArgumentException("Invalid VPA: " + request.getVpa()));

        BigDecimal updatedBalance = account.getAvailableCredit().add(request.getAmount());

        // Ensure available credit does not exceed credit limit
        if (updatedBalance.compareTo(account.getCreditLimit()) > 0) {
            account.setAvailableCredit(account.getCreditLimit());
        } else {
            account.setAvailableCredit(updatedBalance);
        }

        accountRepository.save(account);

        // Record double-entry ledger credit entry for repayment
        LedgerEntry creditEntry = LedgerEntry.builder()
                .accountIdentifier(request.getVpa())
                .entryType(EntryType.CREDIT)
                .amount(request.getAmount())
                .timestamp(LocalDateTime.now())
                .build();

        ledgerRepository.save(creditEntry);

        return new PaymentResponse(null, "SETTLED", "Repayment of " + request.getAmount() + " processed successfully.");
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String vpa) {
        return transactionRepository.findBySenderVpaOrReceiverVpa(vpa, vpa);
    }
}