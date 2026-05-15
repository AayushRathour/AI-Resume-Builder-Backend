package com.resumeai.resume.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleResumeNotFound() {
        ResumeNotFoundException ex = new ResumeNotFoundException("Not found");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resumes/1");
        ResponseEntity<Map<String, Object>> resp = handler.handleResumeNotFound(ex, request);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Not found", resp.getBody().get("message"));
    }

    @Test
    void handleInvalidInput() {
        InvalidInputException ex = new InvalidInputException("Bad input");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resumes");
        ResponseEntity<Map<String, Object>> resp = handler.handleInvalidInput(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Bad input", resp.getBody().get("message"));
    }

    @Test
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        FieldError fe = new FieldError("obj", "title", "must not be null");
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of(fe));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resumes");
        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("title: must not be null", resp.getBody().get("message"));
    }

    @Test
    void handleNotFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/unknown");
        ResponseEntity<Map<String, Object>> resp = handler.handleNotFound(ex, request);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void handleGeneric() {
        Exception ex = new Exception("err");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resumes");
        ResponseEntity<Map<String, Object>> resp = handler.handleGeneric(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }
}
