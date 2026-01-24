package com.nexus_ledger.nexusLedger.kafkaTransaction;

import com.nexus_ledger.nexusLedger.module.Account;
import com.nexus_ledger.nexusLedger.module.IdempotencyRecord;
import com.nexus_ledger.nexusLedger.repository.AccountRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionConsumer {

    private final LedgerService ledgerService;
    private final IdempotencyRepository idempotencyRepo;
    private final FraudSentryService fraudSentryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AccountRepository accountRepository;

    @KafkaListener(topics = "financial-transactions", groupId = "ledger-group")
    public void consume(Map<String, Object> message) {
        String key = (String) message.get("key");
        Map<String, Object> data = (Map<String, Object>) message.get("data");

        log.info("Processing transaction for key: {}", key);

        if (idempotencyRepo.existsById(key)) {
            log.warn("Duplicate transaction detected for key: {}. Skipping...", key);
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(data.get("amount").toString());
            UUID fromId = UUID.fromString(data.get("fromId").toString());
            UUID toId = UUID.fromString(data.get("toId").toString());

            // 2. AI FRAUD ANALYSIS
            boolean isFraud = fraudSentryService.isFraudulent(amount, fromId);

            if (isFraud) {
                log.error("!!! FRAUD ALERT !!! AI blocked transaction {}", key);
                saveIdempotencyRecord(key, "BLOCKED_BY_AI", 403, fromId, toId, amount);

                // NOTIFY UI: Fraud status (Balance won't change)
                sendWsUpdate(fromId, "FRAUD", amount, null);
                return;
            }

            // 3. EXECUTE LEDGER (Balance changes here)
            ledgerService.executeTransfer(fromId, toId, amount, key);

            // 4. FETCH NEW BALANCE
            // We fetch the new balance directly from DB to ensure accuracy
            BigDecimal newBalance = accountRepository.findById(fromId)
                    .map(Account::getBalance)
                    .orElse(BigDecimal.ZERO);

            // 5. SAVE SUCCESS RECORD
            saveIdempotencyRecord(key, "SUCCESS", 200, fromId, toId, amount);

            // 6. NOTIFY UI: Success status + New Balance
            sendWsUpdate(fromId, "SUCCESS", amount, newBalance);

            log.info("Transaction {} processed. New Balance: {}", key, newBalance);

        } catch (Exception e) {
            log.error("Critical failure processing transaction {}: {}", key, e.getMessage());
            // Optionally send an error update
        }
    }

    // HELPER METHOD: This sends the actual WebSocket packet
    private void sendWsUpdate(UUID accountId, String status, BigDecimal amount, BigDecimal newBalance) {
        String destination = "/topic/updates/" + accountId.toString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "TRANSACTION_UPDATE");
        payload.put("status", status);
        payload.put("amount", amount);
        payload.put("newBalance", newBalance); // React will use this to update the UI
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend(destination, payload);
    }

    private void saveIdempotencyRecord(String key, String status, int code, UUID from, UUID to, BigDecimal amt) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setResponseBody(status);
        record.setStatusCode(code);
        record.setFromId(from.toString());
        record.setToId(to.toString());
        record.setAmount(amt);
        idempotencyRepo.save(record);
    }
}