package com.nexus_ledger.nexusLedger.module;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Data
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String ownerName;

    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Version
    private Long version; // Optimistic Locking to prevent double-spending

}
