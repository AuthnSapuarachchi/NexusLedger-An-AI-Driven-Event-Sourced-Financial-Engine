package com.nexus_ledger.nexusLedger.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            System.out.println("--- STARTING NATIVE DATABASE SEED ---");

            // 1. Create Sender (Now including account_name)
            jdbcTemplate.execute(
                    "INSERT INTO accounts (id, account_number, owner_name, account_name, balance, currency, version) " +
                            "VALUES ('550e8400-e29b-41d4-a716-446655440000', 'ACC-SENDER-001', 'System Sender', 'System Main', 1000.00, 'USD', 0) " +
                            "ON CONFLICT (id) DO NOTHING"
            );

            // 2. Create Receiver (Now including account_name)
            jdbcTemplate.execute(
                    "INSERT INTO accounts (id, account_number, owner_name, account_name, balance, currency, version) " +
                            "VALUES ('661f9511-f30c-52e5-b827-557766551111', 'ACC-RECEIVER-002', 'System Receiver', 'System Savings', 500.00, 'USD', 0) " +
                            "ON CONFLICT (id) DO NOTHING"
            );

            System.out.println("--- NATIVE SEED COMPLETE: SYSTEM READY ---");
        };
    }
}