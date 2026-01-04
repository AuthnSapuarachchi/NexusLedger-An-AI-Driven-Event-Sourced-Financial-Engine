package com.nexus_ledger.nexusLedger.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class GlobalExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleConflict() {
        return ResponseEntity.status(409).body("{\"error\": \"Concurrency conflict. Please try again.\"}");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(400).body("{\"error\": \"" + ex.getMessage() + "\"}");
    }

}
