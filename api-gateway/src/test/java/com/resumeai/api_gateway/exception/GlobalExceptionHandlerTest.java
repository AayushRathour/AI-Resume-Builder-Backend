package com.resumeai.api_gateway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.ConnectException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMaxSizeException_Returns413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5000L);
        ResponseEntity<Map<String, String>> response = handler.handleMaxSizeException(ex);

        assertEquals(413, response.getStatusCode().value());
        assertEquals("Payload Too Large", response.getBody().get("error"));
    }

    @Test
    void handleConnectException_Returns503() {
        ConnectException ex = new ConnectException("Connection refused");
        ResponseEntity<Map<String, Object>> response = handler.handleConnectException(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("Service is temporarily unavailable"));
    }

    @Test
    void handleGenericException_ServiceUnavailable_Returns503() {
        Exception ex = new RuntimeException("No instances available for some-service");
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void handleGenericException_Other_Returns500() {
        Exception ex = new RuntimeException("Something went wrong");
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("API Gateway encountered an unexpected error.", response.getBody().get("message"));
    }
}
