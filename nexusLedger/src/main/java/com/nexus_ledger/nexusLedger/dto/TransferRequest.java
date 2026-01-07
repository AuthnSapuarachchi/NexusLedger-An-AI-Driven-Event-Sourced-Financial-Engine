package com.nexus_ledger.nexusLedger.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    private String fromId;
    private String toId;
    private BigDecimal amount;

}
