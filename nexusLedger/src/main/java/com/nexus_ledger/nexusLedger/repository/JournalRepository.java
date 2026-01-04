package com.nexus_ledger.nexusLedger.repository;

import com.nexus_ledger.nexusLedger.module.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<JournalEntry, Long> { }
