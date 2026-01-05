package com.nexus_ledger.nexusLedger.controller;

import com.nexus_ledger.nexusLedger.dto.TransferRequest;
import com.nexus_ledger.nexusLedger.kafkaTransaction.TransactionProducer;
import com.nexus_ledger.nexusLedger.module.IdempotencyRecord;
import com.nexus_ledger.nexusLedger.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final TransactionProducer transactionProducer;
    private final IdempotencyRepository idempotencyRepo;

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(
            @RequestHeader("X-Idempotency-Key") String key,
            @RequestBody TransferRequest request) {

        // 1. Validate the request briefly (Industry best practice)
        if (request.getAmount().doubleValue() <= 0) {
            return ResponseEntity.badRequest().body("{\"error\": \"Amount must be positive\"}");
        }

        // 2. Push the "Intent" to Kafka
        // This is non-blocking and extremely fast.
        transactionProducer.sendTransaction(request, key);

        // 3. Return 202 Accepted
        // We tell the user "We got it and are working on it."
        return ResponseEntity.accepted().body("{\"message\": \"Transaction queued for processing\", \"idempotencyKey\": \"" + key + "\"}");
    }

    @GetMapping("/history")
    public ResponseEntity<List<IdempotencyRecord>> getHistory() {
        // We return the records so the frontend can see SUCCESS vs FRAUD
        return ResponseEntity.ok(idempotencyRepo.findAll());
    }

}
