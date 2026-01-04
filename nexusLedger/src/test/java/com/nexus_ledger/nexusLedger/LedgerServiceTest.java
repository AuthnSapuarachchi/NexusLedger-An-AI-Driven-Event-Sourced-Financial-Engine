package com.nexus_ledger.nexusLedger;

import com.nexus_ledger.nexusLedger.repository.AccountRepository;
import com.nexus_ledger.nexusLedger.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class LedgerServiceTest {

    @Autowired
    private LedgerService ledgerService;
    @Autowired private AccountRepository accountRepo;

    @Test
    void testAtomicTransfer() {
        // 1. Setup two accounts
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        // Save these accounts to your DB first with 1000 USD balance

        // 2. Try to move 500 USD
        ledgerService.executeTransfer(fromId, toId, new BigDecimal("500"), "TX-123");

        // 3. Assert balances
        assertEquals(0, new BigDecimal("500").compareTo(accountRepo.findById(fromId).get().getBalance()));
        assertEquals(0, new BigDecimal("1500").compareTo(accountRepo.findById(toId).get().getBalance()));
    }

}
