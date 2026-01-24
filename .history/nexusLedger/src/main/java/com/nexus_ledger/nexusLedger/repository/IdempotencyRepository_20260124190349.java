package com.nexus_ledger.nexusLedger.repository;

import com.nexus_ledger.nexusLedger.module.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    List<IdempotencyRecord> findByFromId(String fromId);
    List<IdempotencyRecord> findByFromIdIn(List<String> fromIds);
}