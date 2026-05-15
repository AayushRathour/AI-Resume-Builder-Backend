package com.resumeai.auth.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Centralized exception handler for consistent API error responses. */
@RestControllerAdvice
public class FeignErrorAdvice {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null || status.is5xxServerError()) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "error", "Upstream service unavailable",
                "message", "Dependent service call failed. Please try again shortly.",
                "path", request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}

