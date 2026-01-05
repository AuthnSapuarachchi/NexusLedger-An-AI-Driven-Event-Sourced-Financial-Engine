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
    public ResponseEntity<?> transfer(
            @RequestHeader("X-Idempotency-Key") String key,
            @RequestBody TransferRequest request) {

        // LOGGING: See exactly what React is sending
        System.out.println("Received Request: " + request);

        // GUARD CLUASE: Prevent the NullPointerException
        if (request == null || request.getFromId() == null || request.getToId() == null || request.getAmount() == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Missing required fields: fromId, toId, or amount\"}");
        }

        try {
            transactionProducer.sendTransaction(request, key);
            return ResponseEntity.accepted().body("{\"message\": \"Transaction queued\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<IdempotencyRecord>> getHistory() {
        // We return the records so the frontend can see SUCCESS vs FRAUD
        return ResponseEntity.ok(idempotencyRepo.findAll());
    }

}
