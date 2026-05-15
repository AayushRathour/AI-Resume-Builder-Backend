package com.resumeai.notification;

import com.resumeai.notification.controller.HealthController;
import com.resumeai.notification.exception.GlobalExceptionHandler;
import com.resumeai.notification.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OtherNotificationTests {

    @Test
    void healthController_Root() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, String>> response = controller.root();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("notification-service", response.getBody().get("service"));
    }

    @Test
    void healthController_Health() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, String>> response = controller.health();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void globalExceptionHandler_ResourceNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found", UUID.randomUUID());
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("Not found"));
    }

    @Test
    void globalExceptionHandler_Generic() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception ex = new Exception("Error");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
