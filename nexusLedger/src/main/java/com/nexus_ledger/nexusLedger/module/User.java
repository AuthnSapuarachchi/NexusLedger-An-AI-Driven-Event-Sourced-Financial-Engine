package com.nexus_ledger.nexusLedger.module;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String githubId; // To store the unique ID from GitHub

    private String name;
    private String email;

    @OneToOne
    @JoinColumn(name = "account_id")
    private Account account;

}
