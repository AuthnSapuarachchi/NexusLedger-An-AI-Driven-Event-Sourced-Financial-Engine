package com.nexus_ledger.nexusLedger.module;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Data
public class Account {

    @Id
    private UUID id;
    private String accountNumber;
    private BigDecimal balance;

    @Version
    private Long version; // Optimistic Locking to prevent double-spending

}
