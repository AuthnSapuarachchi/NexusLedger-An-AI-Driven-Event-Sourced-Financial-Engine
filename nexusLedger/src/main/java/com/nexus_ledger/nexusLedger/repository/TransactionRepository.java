package com.nexus_ledger.nexusLedger.repository;

import com.nexus_ledger.nexusLedger.module.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> { }