package com.nexus_ledger.nexusLedger.controller;

import com.nexus_ledger.nexusLedger.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepo;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String githubId = principal.getAttribute("id").toString();

        return userRepo.findByGithubId(githubId)
                .map(user -> {
                    // Fix: Use a Stream of Maps and explicitly collect them
                    List<Map<String, Object>> accountList = user.getAccounts().stream()
                            .map(acc -> {
                                // Explicitly create a Map<String, Object> to avoid inference issues
                                Map<String, Object> m = new java.util.HashMap<>();
                                m.put("id", acc.getId());
                                m.put("accountName", acc.getAccountName());
                                m.put("accountNumber", acc.getAccountNumber());
                                m.put("balance", acc.getBalance());
                                m.put("currency", acc.getCurrency());
                                return m;
                            })
                            .collect(Collectors.toList());

                    // Construct the final response
                    Map<String, Object> responseBody = new java.util.HashMap<>();
                    responseBody.put("name", user.getName());
                    responseBody.put("email", user.getEmail());
                    responseBody.put("accounts", accountList);
                    
                    // Add accountId and balance from the first account for frontend compatibility
                    if (!accountList.isEmpty()) {
                        Map<String, Object> firstAccount = accountList.get(0);
                        responseBody.put("accountId", firstAccount.get("id"));
                        responseBody.put("balance", firstAccount.get("balance"));
                    }

                    return ResponseEntity.ok(responseBody);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, null);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}