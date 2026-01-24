package com.nexus_ledger.nexusLedger.security;

import com.nexus_ledger.nexusLedger.module.Account;
import com.nexus_ledger.nexusLedger.module.User;
import com.nexus_ledger.nexusLedger.repository.AccountRepository;
import com.nexus_ledger.nexusLedger.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepo;
    private final AccountRepository accountRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // 1. Defensively extract attributes
        String githubId = oauthUser.getAttribute("id") != null ?
                oauthUser.getAttribute("id").toString() : null;

        String email = oauthUser.getAttribute("email");
        if (email == null) email = oauthUser.getAttribute("login") + "@github.temp"; // Fallback

        String name = oauthUser.getAttribute("name");
        if (name == null) name = oauthUser.getAttribute("login"); // Fallback to username

        final String finalEmail = email;
        final String finalName = name;

        // 2. Logic to find or create
        User user = userRepo.findByGithubId(githubId)
                .orElseGet(() -> {
                    System.out.println("No user found. Provisioning new Identity and Main Vault...");

                    // 1. Create the User first (without the account yet)
                    User newUser = new User();
                    newUser.setGithubId(githubId);
                    newUser.setName(finalName);
                    newUser.setEmail(finalEmail);

                    // 2. Create the first "Main" Vault for this user
                    Account mainAccount = new Account();
                    mainAccount.setId(UUID.randomUUID());
                    mainAccount.setAccountName("Main"); // The vault name
                    mainAccount.setAccountNumber("ACC-" + githubId);
                    mainAccount.setBalance(new BigDecimal("1000.00"));
                    mainAccount.setOwnerName(finalName);
                    mainAccount.setCurrency("USD");

                    // 3. Link the Account to the User
                    mainAccount.setOwner(newUser);

                    // 4. Add the Account to the User's list
                    // This replaces .setAccount(savedAccount)
                    newUser.getAccounts().add(mainAccount);

                    // 5. Save the User (CascadeType.ALL will save the Account automatically)
                    return userRepo.save(newUser);
                });

        System.out.println("Login Success for: " + user.getEmail());

        // 3. Redirect to React
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:5173/dashboard");
    }
}
