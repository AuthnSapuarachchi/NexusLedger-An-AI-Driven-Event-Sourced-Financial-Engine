package com.nexus_ledger.nexusLedger.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {

    private UUID fromId;
    private UUID toId;
    private BigDecimal amount;

}
