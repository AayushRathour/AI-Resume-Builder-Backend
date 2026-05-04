package com.resumeai.jobmatch.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errors);
        return buildError(HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return buildError(status, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // STEP 1: COMPREHENSIVE ERROR LOGGING
        log.error("\n===== CRITICAL ERROR OCCURRED =====");
        log.error("Exception Type: {}", ex.getClass().getSimpleName());
        log.error("Error Message: {}", ex.getMessage());
        log.error("Stack Trace:");
        ex.printStackTrace();
        
        // Log root cause
        Throwable rootCause = ex;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
            log.error("Root Cause: {} - {}", rootCause.getClass().getSimpleName(), rootCause.getMessage());
        }
        log.error("====================================\n");
        
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
