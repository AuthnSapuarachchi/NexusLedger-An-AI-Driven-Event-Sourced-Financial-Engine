package com.nexus_ledger.nexusLedger.intercepter;

import com.nexus_ledger.nexusLedger.repository.IdempotencyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {
    private final IdempotencyRepository repository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = request.getHeader("X-Idempotency-Key");
        if (key == null || key.isBlank()) return true;

        var record = repository.findById(key);
        if (record.isPresent()) {
            response.setStatus(record.get().getStatusCode());
            response.setContentType("application/json");
            response.getWriter().write(record.get().getResponseBody());
            return false; // Stop the request here
        }
        return true;
    }

}
