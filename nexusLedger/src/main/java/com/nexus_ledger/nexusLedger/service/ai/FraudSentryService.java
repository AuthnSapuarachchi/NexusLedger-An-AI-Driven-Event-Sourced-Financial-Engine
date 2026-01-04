package com.nexus_ledger.nexusLedger.service.ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class FraudSentryService {

    private final OllamaChatModel chatModel;
    private final Counter fraudCounter;

    // We use a manual constructor to initialize the Micrometer Counter correctly
    public FraudSentryService(OllamaChatModel chatModel, MeterRegistry registry) {
        this.chatModel = chatModel;
        this.fraudCounter = Counter.builder("ledger.fraud.detected")
                .description("Number of fraud transactions blocked by AI")
                .register(registry);
    }

    public boolean isFraudulent(BigDecimal amount, UUID fromId) {
        log.info("AI Sentry analyzing transaction: Account {} moving ${}", fromId, amount);

        String prompt = """
            You are a Financial Fraud Detection AI. 
            Analyze this transaction: Account %s is attempting to move $%s.
            Rule: Any transaction over $1000 is considered HIGH RISK.
            Respond with ONLY one word: 'SAFE' or 'FRAUD'.
            """.formatted(fromId, amount);

        try {
            String response = chatModel.call(prompt).trim().toUpperCase();
            log.info("AI Analysis Result: {}", response);

            // LOGIC FIX: Check for fraud, increment counter, THEN return
            if (response.contains("FRAUD")) {
                fraudCounter.increment();
                return true;
            }

            return false; // It was safe

        } catch (Exception e) {
            log.error("AI Sentry offline! Defaulting to Safe Mode: {}", e.getMessage());
            return false;
        }
    }
}
