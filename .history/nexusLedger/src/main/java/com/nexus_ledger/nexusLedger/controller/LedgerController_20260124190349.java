package com.nexus_ledger.nexusLedger.controller;

import com.nexus_ledger.nexusLedger.dto.TransferRequest;
import com.nexus_ledger.nexusLedger.kafkaTransaction.TransactionProducer;
import com.nexus_ledger.nexusLedger.module.Account;
import com.nexus_ledger.nexusLedger.module.IdempotencyRecord;
import com.nexus_ledger.nexusLedger.module.User;
import com.nexus_ledger.nexusLedger.repository.AccountRepository;
import com.nexus_ledger.nexusLedger.repository.IdempotencyRepository;
import com.nexus_ledger.nexusLedger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final TransactionProducer transactionProducer;
    private final IdempotencyRepository idempotencyRepo;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository; // Add this

    @PostMapping("/accounts/create")
    public ResponseEntity<?> createVault(
            @RequestParam String vaultName,
            @AuthenticationPrincipal OAuth2User principal) {

        String githubId = principal.getAttribute("id").toString();
        User user = userRepository.findByGithubId(githubId).orElseThrow();

        Account vault = new Account();
        vault.setId(UUID.randomUUID());
        vault.setAccountName(vaultName);
        vault.setAccountNumber("VLT-" + UUID.randomUUID().toString().substring(0,8));
        vault.setOwner(user);
        vault.setBalance(BigDecimal.ZERO);
        vault.setOwnerName(user.getName());

        accountRepository.save(vault);
        return ResponseEntity.ok("Vault '" + vaultName + "' created successfully!");
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @RequestHeader("X-Idempotency-Key") String key,
            @RequestBody TransferRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        String githubId = principal.getAttribute("id").toString();
        User currentUser = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new RuntimeException("User not found in system"));

        // --- NEW LOGIC: Verify if the 'fromId' belongs to ANY of the user's accounts ---
        boolean ownsAccount = currentUser.getAccounts().stream()
                .anyMatch(acc -> acc.getId().toString().equals(request.getFromId()));

        if (!ownsAccount) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"message\": \"Security Violation: You do not own the source account!\"}");
        }

        if (request.getToId() == null || request.getAmount() == null) {
            return ResponseEntity.badRequest().body("{\"message\": \"Missing required fields\"}");
        }

        try {
            transactionProducer.sendTransaction(request, key);
            return ResponseEntity.accepted().body("{\"message\": \"Transaction queued\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<IdempotencyRecord>> getHistory(@AuthenticationPrincipal OAuth2User principal) {
        String githubId = principal.getAttribute("id").toString();
        User currentUser = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Collect history for ALL accounts owned by this user
        List<String> myAccountIds = currentUser.getAccounts().stream()
                .map(acc -> acc.getId().toString())
                .toList();

        if (myAccountIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // Query all transactions where fromId matches any of the user's account IDs
        List<IdempotencyRecord> myHistory = idempotencyRepo.findByFromIdIn(myAccountIds);

        return ResponseEntity.ok(myHistory);
    }
}