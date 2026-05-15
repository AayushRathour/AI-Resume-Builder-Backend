package com.resumeai.export.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Centralized exception handler for consistent API error responses. */

@RestControllerAdvice
public class FeignExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_GATEWAY.value());
        body.put("error", "Bad Gateway");
        body.put("message", "Upstream service call failed");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}

