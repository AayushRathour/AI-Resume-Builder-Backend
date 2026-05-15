package com.resumeai.jobmatch.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "field", "error")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/jobmatch");
        ResponseEntity<Map<String, Object>> resp = handler.handleValidationErrors(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().get("message").toString().contains("field: error"));
    }

    @Test
    void handleNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/jobmatch/1");
        ResponseEntity<Map<String, Object>> resp = handler.handleNotFound(ex, request);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Not found", resp.getBody().get("message"));
    }

    @Test
    void handleResponseStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden reason");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/jobmatch");
        ResponseEntity<Map<String, Object>> resp = handler.handleResponseStatus(ex, request);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("Forbidden reason", resp.getBody().get("message"));
    }

    @Test
    void handleMaxUploadSize() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(100L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/jobmatch/parse-resume");
        ResponseEntity<Map<String, Object>> resp = handler.handleMaxUploadSize(ex, request);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, resp.getStatusCode());
    }

    @Test
    void handleGeneric() {
        Exception ex = new Exception("Generic error");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/jobmatch");
        ResponseEntity<Map<String, Object>> resp = handler.handleGeneric(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("Unexpected error occurred", resp.getBody().get("message"));
    }
}
