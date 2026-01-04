package com.nexus_ledger.nexusLedger.kafkaTransaction;

import com.nexus_ledger.nexusLedger.module.IdempotencyRecord;
import com.nexus_ledger.nexusLedger.repository.IdempotencyRepository;
import com.nexus_ledger.nexusLedger.service.LedgerService;
import com.nexus_ledger.nexusLedger.service.ai.FraudSentryService; // Import the AI Service
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final LedgerService ledgerService;
    private final IdempotencyRepository idempotencyRepo;
    private final FraudSentryService fraudSentryService; // Inject the AI Sentry

    @KafkaListener(topics = "financial-transactions", groupId = "ledger-group")
    public void consume(Map<String, Object> message) {
        String key = (String) message.get("key");
        Map<String, Object> data = (Map<String, Object>) message.get("data");

        log.info("Processing transaction for key: {}", key);

        // 1. IDEMPOTENCY CHECK (Safety)
        if (idempotencyRepo.existsById(key)) {
            log.warn("Duplicate transaction detected for key: {}. Skipping...", key);
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(data.get("amount").toString());
            UUID fromId = UUID.fromString(data.get("fromId").toString());
            UUID toId = UUID.fromString(data.get("toId").toString());

            // 2. AI FRAUD ANALYSIS (Intelligence)
            // We call Ollama here to check the risk
            boolean isFraud = fraudSentryService.isFraudulent(amount, fromId);

            if (isFraud) {
                log.error("!!! FRAUD ALERT !!! AI blocked transaction {} for amount ${}", key, amount);
                saveIdempotencyRecord(key, "BLOCKED_BY_AI", 403);
                return; // Stop here! Do not move money.
            }

            // 3. EXECUTE LEDGER (Heart)
            ledgerService.executeTransfer(fromId, toId, amount, key);

            // 4. SAVE SUCCESS RECORD
            saveIdempotencyRecord(key, "{\"message\": \"Processed via Kafka\"}", 200);
            log.info("Transaction {} processed successfully.", key);

        } catch (Exception e) {
            log.error("Critical failure processing transaction {}: {}", key, e.getMessage());
        }
    }

    // Helper method to keep code clean
    private void saveIdempotencyRecord(String key, String body, int code) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setResponseBody(body);
        record.setStatusCode(code);
        idempotencyRepo.save(record);
    }

}
