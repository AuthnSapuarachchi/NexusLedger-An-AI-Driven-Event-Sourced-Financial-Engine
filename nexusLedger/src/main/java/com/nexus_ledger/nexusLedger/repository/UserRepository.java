package com.nexus_ledger.nexusLedger.repository;

import com.nexus_ledger.nexusLedger.module.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring generates: SELECT * FROM users WHERE github_id = ?
    Optional<User> findByGithubId(String githubId);
}
