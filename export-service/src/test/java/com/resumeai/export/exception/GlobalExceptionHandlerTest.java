package com.resumeai.export.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_returnsFieldErrorMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "format", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/exports");
        ResponseEntity<Map<String, Object>> result = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertTrue(result.getBody().get("message").toString().contains("format"));
        assertTrue(result.getBody().get("message").toString().contains("must not be null"));
        assertEquals(400, result.getBody().get("status"));
        assertNotNull(result.getBody().get("timestamp"));
    }

    @Test
    void handleValidation_noFieldErrors_returnsFallbackMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/exports");
        ResponseEntity<Map<String, Object>> result = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Validation failed", result.getBody().get("message"));
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        Exception ex = new RuntimeException("Something broke");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/exports");
        ResponseEntity<Map<String, Object>> result = handler.handleGeneric(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Unexpected error occurred", result.getBody().get("message"));
        assertEquals(500, result.getBody().get("status"));
        assertEquals("Internal Server Error", result.getBody().get("error"));
        assertNotNull(result.getBody().get("timestamp"));
    }
}
