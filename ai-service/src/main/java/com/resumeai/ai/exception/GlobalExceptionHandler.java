package com.resumeai.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Payload Too Large");
        errorResponse.put("message", "File size exceeds the 5MB limit. Please upload a smaller file.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception e) {
        e.printStackTrace();
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "failed");
        payload.put("message", e.getMessage() == null ? "Unexpected error" : e.getMessage());
        return ResponseEntity.ok(payload);
    }
}
