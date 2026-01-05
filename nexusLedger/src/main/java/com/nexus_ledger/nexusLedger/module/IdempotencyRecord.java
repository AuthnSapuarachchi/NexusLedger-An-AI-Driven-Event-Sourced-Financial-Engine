package com.nexus_ledger.nexusLedger.module;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "idempotency_records")
@Data
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;
    private String responseBody;
    private int statusCode;

    private String fromId;
    private String toId;
    private BigDecimal amount;

}
