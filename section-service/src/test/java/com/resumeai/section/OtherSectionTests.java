package com.resumeai.section;

import com.resumeai.section.controller.ServiceStatusController;
import com.resumeai.section.exception.GlobalExceptionHandler;
import com.resumeai.section.exception.InvalidInputException;
import com.resumeai.section.exception.SectionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OtherSectionTests {

    @Test
    void serviceStatusController_Root() {
        ServiceStatusController controller = new ServiceStatusController();
        ResponseEntity<Map<String, String>> response = controller.root();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("section-service", response.getBody().get("service"));
    }

    @Test
    void serviceStatusController_Health() {
        ServiceStatusController controller = new ServiceStatusController();
        ResponseEntity<Map<String, String>> response = controller.health();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void globalExceptionHandler_SectionNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        SectionNotFoundException ex = new SectionNotFoundException("Not found");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/sections/1");
        ResponseEntity<Map<String, Object>> response = handler.handleSectionNotFound(ex, request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", response.getBody().get("message"));
    }

    @Test
    void globalExceptionHandler_InvalidInput() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        InvalidInputException ex = new InvalidInputException("Invalid");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/sections");
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidInput(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid", response.getBody().get("message"));
    }

    @Test
    void globalExceptionHandler_Generic() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception ex = new Exception("Error");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/sections");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
