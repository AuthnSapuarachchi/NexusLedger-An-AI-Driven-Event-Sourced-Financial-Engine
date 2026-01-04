package com.nexus_ledger.nexusLedger.service;

import com.nexus_ledger.nexusLedger.module.Account;
import com.nexus_ledger.nexusLedger.module.JournalEntry;
import com.nexus_ledger.nexusLedger.module.Transaction;
import com.nexus_ledger.nexusLedger.repository.AccountRepository;
import com.nexus_ledger.nexusLedger.repository.JournalRepository;
import com.nexus_ledger.nexusLedger.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;
    private final JournalRepository journalRepo;

    @Transactional
    public void executeTransfer(UUID fromId, UUID toId, BigDecimal amount, String ref) {
        // 1. Validation
        Account from = accountRepo.findById(fromId).orElseThrow();
        if (from.getBalance().compareTo(amount) < 0) throw new RuntimeException("Insufficient Funds");

        // 2. Create Header
        Transaction tx = txRepo.save(new Transaction(UUID.randomUUID(), "Transfer", ref, LocalDateTime.now()));

        // 3. Double-Entry Legs
        journalRepo.save(new JournalEntry(null, tx, fromId, amount.negate()));
        journalRepo.save(new JournalEntry(null, tx, toId, amount));

        // 4. Update Balances (Atomic)
        accountRepo.updateBalance(fromId, amount.negate());
        accountRepo.updateBalance(toId, amount);
    }

}
