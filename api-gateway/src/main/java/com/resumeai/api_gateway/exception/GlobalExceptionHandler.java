package com.resumeai.api_gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps gateway exceptions to consistent error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles multipart size violations at the gateway.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Payload Too Large");
        errorResponse.put("message", "File size exceeds the 5MB limit. Please upload a smaller file.");
        return ResponseEntity.status(413).body(errorResponse);
    }

    /**
     * Handles downstream connection failures gracefully.
     */
    @ExceptionHandler(java.net.ConnectException.class)
    public ResponseEntity<Map<String, Object>> handleConnectException(java.net.ConnectException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service is temporarily unavailable or starting up. Please try again in a few seconds.");
    }

    /**
     * Fallback handler for unexpected gateway exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        String message = ex.getMessage();
        if (message != null && (message.contains("Service Unavailable") || message.contains("No instances available"))) {
            return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service is temporarily unavailable or starting up. Please try again in a few seconds.");
        }
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "API Gateway encountered an unexpected error.");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", java.time.LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
