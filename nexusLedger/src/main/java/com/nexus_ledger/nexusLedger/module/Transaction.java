package com.nexus_ledger.nexusLedger.module;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    private UUID id;
    private String description;
    private String referenceId; // External ID from the client
    private LocalDateTime createdAt = LocalDateTime.now();

}
