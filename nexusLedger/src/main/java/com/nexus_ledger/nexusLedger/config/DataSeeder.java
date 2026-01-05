package com.nexus_ledger.nexusLedger.config;

import com.nexus_ledger.nexusLedger.module.Account;
import com.nexus_ledger.nexusLedger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class DataSeeder { // Removed "implements CommandLineRunner"

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            System.out.println("--- STARTING NATIVE DATABASE SEED ---");

            // 1. Create Sender if not exists
            jdbcTemplate.execute(
                    "INSERT INTO accounts (id, account_number, balance, version) " +
                            "VALUES ('550e8400-e29b-41d4-a716-446655440000', 'ACC-SENDER-001', 1000.00, 0) " +
                            "ON CONFLICT (id) DO NOTHING"
            );

            // 2. Create Receiver if not exists
            jdbcTemplate.execute(
                    "INSERT INTO accounts (id, account_number, balance, version) " +
                            "VALUES ('661f9511-f30c-52e5-b827-557766551111', 'ACC-RECEIVER-002', 500.00, 0) " +
                            "ON CONFLICT (id) DO NOTHING"
            );

            System.out.println("--- NATIVE SEED COMPLETE: SYSTEM READY ---");
        };
    }
}
