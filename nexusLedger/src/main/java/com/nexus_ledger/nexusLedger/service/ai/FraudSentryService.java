package com.nexus_ledger.nexusLedger.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudSentryService {

    private final OllamaChatModel chatModel;

    public boolean isFraudulent(BigDecimal amount, UUID fromId) {
        log.info("AI Sentry analyzing transaction: Account {} moving ${}", fromId, amount);

        // This prompt instructs the AI to act as a Risk Officer
        String prompt = """
            You are a Financial Fraud Detection AI. 
            Analyze this transaction: Account %s is attempting to move $%s.
            Rule: Any transaction over $1000 is considered HIGH RISK.
            Respond with ONLY one word: 'SAFE' or 'FRAUD'.
            """.formatted(fromId, amount);

        try {
            String response = chatModel.call(prompt).trim().toUpperCase();
            log.info("AI Analysis Result: {}", response);
            return response.contains("FRAUD");
        } catch (Exception e) {
            log.error("AI Sentry offline! Defaulting to Safe Mode: {}", e.getMessage());
            return false; // Fail-safe: don't block everything if AI is down
        }
    }

}
