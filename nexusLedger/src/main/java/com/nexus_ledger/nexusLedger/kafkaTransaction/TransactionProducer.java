package com.nexus_ledger.nexusLedger.kafkaTransaction;

import com.nexus_ledger.nexusLedger.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "financial-transactions";

    public void sendTransaction(TransferRequest request, String idempotencyKey) {
        // We send the whole request + the key
        Map<String, Object> message = new HashMap<>();
        message.put("data", request);
        message.put("key", idempotencyKey);

        kafkaTemplate.send(TOPIC, idempotencyKey, message);
    }

}
