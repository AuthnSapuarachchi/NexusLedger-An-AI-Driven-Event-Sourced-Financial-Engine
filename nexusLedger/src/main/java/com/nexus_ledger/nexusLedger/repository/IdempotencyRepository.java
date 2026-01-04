package com.nexus_ledger.nexusLedger.repository;

import com.nexus_ledger.nexusLedger.module.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> { }