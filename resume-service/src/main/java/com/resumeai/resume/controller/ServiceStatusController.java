package com.resumeai.resume.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health endpoints for infrastructure checks.
 */
@RestController
public class ServiceStatusController {

    /**
     * Root status probe for service discovery checks.
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "resume-service",
                "status", "UP"));
    }

    /**
     * Health endpoint for readiness/liveness checks.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "resume-service",
                "status", "UP"));
    }
}
