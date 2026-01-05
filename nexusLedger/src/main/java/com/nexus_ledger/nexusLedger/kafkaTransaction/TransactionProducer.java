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

        Map<String, Object> data = new HashMap<>();

        // Using String.valueOf() is safer than .toString() because it handles nulls
        data.put("fromId", String.valueOf(request.getFromId()));
        data.put("toId", String.valueOf(request.getToId()));
        data.put("amount", request.getAmount());

        message.put("data", data);

        kafkaTemplate.send("financial-transactions", key, message);
    }

}
