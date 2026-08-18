package com.fintech.upisimulator.repository;

import com.fintech.upisimulator.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {}