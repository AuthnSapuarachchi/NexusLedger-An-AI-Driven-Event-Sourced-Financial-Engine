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

    public void sendTransaction(TransferRequest request, String key) {
        Map<String, Object> message = new HashMap<>();
        message.put("key", key);

        // This is the "data" map your Consumer is looking for!
        Map<String, Object> data = new HashMap<>();
        data.put("fromId", request.getFromId().toString());
        data.put("toId", request.getToId().toString());
        data.put("amount", request.getAmount());

        message.put("data", data);

        kafkaTemplate.send("financial-transactions", key, message);
    }

}
