package com.nexus_ledger.nexusLedger.repository;

import com.nexus_ledger.nexusLedger.module.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.id = :id")
    void updateBalance(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}