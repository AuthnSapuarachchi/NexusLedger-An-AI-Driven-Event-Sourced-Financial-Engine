package com.nexus_ledger.nexusLedger.kafkaTransaction;

import com.nexus_ledger.nexusLedger.module.IdempotencyRecord;
import com.nexus_ledger.nexusLedger.repository.IdempotencyRepository;
import com.nexus_ledger.nexusLedger.service.LedgerService;
import com.nexus_ledger.nexusLedger.service.ai.FraudSentryService; // Import the AI Service
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final LedgerService ledgerService;
    private final IdempotencyRepository idempotencyRepo;
    private final FraudSentryService fraudSentryService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "financial-transactions", groupId = "ledger-group")
    public void consume(Map<String, Object> message) {
        String key = (String) message.get("key");
        Map<String, Object> data = (Map<String, Object>) message.get("data");

        log.info("Processing transaction for key: {}", key);

        // 1. IDEMPOTENCY CHECK
        if (idempotencyRepo.existsById(key)) {
            log.warn("Duplicate transaction detected for key: {}. Skipping...", key);
            return;
        }

        try {
            // Extract data
            BigDecimal amount = new BigDecimal(data.get("amount").toString());
            UUID fromId = UUID.fromString(data.get("fromId").toString());
            UUID toId = UUID.fromString(data.get("toId").toString());

            // 2. AI FRAUD ANALYSIS
            boolean isFraud = fraudSentryService.isFraudulent(amount, fromId);

            if (isFraud) {
                log.error("!!! FRAUD ALERT !!! AI blocked transaction {} for amount ${}", key, amount);

                // Save record with full data for history
                saveIdempotencyRecord(key, "BLOCKED_BY_AI", 403, fromId, toId, amount);

                // Notify UI of FRAUD
                sendWsUpdate(key, "FRAUD", amount);
                return;
            }

            // 3. EXECUTE LEDGER
            ledgerService.executeTransfer(fromId, toId, amount, key);

            // 4. SAVE SUCCESS RECORD
            saveIdempotencyRecord(key, "SUCCESS", 200, fromId, toId, amount);

            // Notify UI of SUCCESS
            sendWsUpdate(key, "SUCCESS", amount);

            log.info("Transaction {} processed successfully.", key);

        } catch (Exception e) {
            log.error("Critical failure processing transaction {}: {}", key, e.getMessage());
            // Optionally notify UI of ERROR
            sendWsUpdate(key, "ERROR", BigDecimal.ZERO);
        }
    }

    // Helper to notify React via WebSocket
    private void sendWsUpdate(String key, String status, BigDecimal amount) {
        Map<String, Object> update = new HashMap<>();
        update.put("id", key);
        update.put("status", status);
        update.put("amount", amount);
        messagingTemplate.convertAndSend("/topic/transactions", update);
    }

    // Updated helper method to match your new fields
    private void saveIdempotencyRecord(String key, String body, int code, UUID from, UUID to, BigDecimal amt) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setResponseBody(body);
        record.setStatusCode(code);
        record.setFromId(from.toString());
        record.setToId(to.toString());
        record.setAmount(amt);
        idempotencyRepo.save(record);
    }

}
