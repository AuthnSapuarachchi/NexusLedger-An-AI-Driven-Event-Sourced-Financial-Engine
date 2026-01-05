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
        // 1. Validation - Fetch both accounts safely
        Account from = accountRepo.findById(fromId)
                .orElseThrow(() -> new RuntimeException("Sender account not found: " + fromId));

        Account to = accountRepo.findById(toId)
                .orElseThrow(() -> new RuntimeException("Receiver account not found: " + toId));

        // 2. Check Funds
        if (from.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient Funds in account: " + fromId);
        }

        // 3. Create Transaction Header
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setDescription("TRANSFER"); // Maps to your 'description' field
        tx.setReferenceId(ref);        // Maps to your 'referenceId' field
        // Note: createdAt is already initialized to LocalDateTime.now() in your Entity
        tx = txRepo.save(tx);

        // 4. Double-Entry Legs (Atomic Persistence)
        // Debit the Sender
        journalRepo.save(new JournalEntry(null, tx, fromId, amount.negate()));
        // Credit the Receiver
        journalRepo.save(new JournalEntry(null, tx, toId, amount));

        // 5. Update Balances (Using your Repository's atomic update)
        accountRepo.updateBalance(fromId, amount.negate());
        accountRepo.updateBalance(toId, amount);

        System.out.println("Successfully moved $" + amount + " from " + fromId + " to " + toId);
    }

}
