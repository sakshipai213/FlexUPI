package com.fintech.upisimulator.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;
    private String accountIdentifier;

    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    private BigDecimal amount;
    private LocalDateTime timestamp;
}