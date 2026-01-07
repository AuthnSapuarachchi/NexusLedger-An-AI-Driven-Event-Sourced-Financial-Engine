package com.nexus_ledger.nexusLedger.controller;

import com.nexus_ledger.nexusLedger.dto.TransferRequest;
import com.nexus_ledger.nexusLedger.kafkaTransaction.TransactionProducer;
import com.nexus_ledger.nexusLedger.module.IdempotencyRecord;
import com.nexus_ledger.nexusLedger.module.User;
import com.nexus_ledger.nexusLedger.repository.IdempotencyRepository;
import com.nexus_ledger.nexusLedger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @RequestHeader("X-Idempotency-Key") String key,
            @RequestBody TransferRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        // 1. Log the incoming data
        System.out.println("DEBUG: Key=" + key + " | From=" + request.getFromId() + " | To=" + request.getToId() + " | Amt=" + request.getAmount());

        String githubId = principal.getAttribute("id").toString();
        User currentUser = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new RuntimeException("User not found in system"));

        // 2. THE FIX: Convert UUID to String for comparison
        String actualOwnerId = currentUser.getAccount().getId().toString();
        String requestedFromId = request.getFromId();

        System.out.println("COMPARE: Requested=" + requestedFromId + " | Actual=" + actualOwnerId);

        if (!requestedFromId.equals(actualOwnerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"message\": \"Security Violation: Account ownership mismatch!\"}");
        }

        // 3. Validation
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
        // 1. Get the logged-in user
        String githubId = principal.getAttribute("id").toString();
        User currentUser = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Fetch only THEIR records
        String myId = currentUser.getAccount().getId().toString();
        List<IdempotencyRecord> myHistory = idempotencyRepo.findByFromId(myId);

        return ResponseEntity.ok(myHistory);
    }

}
