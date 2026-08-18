package com.fintech.upisimulator.repository;

import com.fintech.upisimulator.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findBySenderVpaOrReceiverVpa(String senderVpa, String receiverVpa);
}