package com.resumeai.ai.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleMaxSizeException() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5000L);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/ai/upload");
        ResponseEntity<Map<String, Object>> resp = handler.handleMaxSizeException(ex, request);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, resp.getStatusCode());
        assertTrue(resp.getBody().get("message").toString().contains("limit"));
    }

    @Test
    void handleGenericException() {
        Exception ex = new Exception("Test err");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/ai/generate");
        ResponseEntity<Map<String, Object>> resp = handler.handle(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("Unexpected error occurred", resp.getBody().get("message"));
    }
}
