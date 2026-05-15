package com.resumeai.api_gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTests {

    @Test
    void swaggerDummyController_ReturnsStatus() {
        SwaggerDummyController controller = new SwaggerDummyController();
        assertEquals("Gateway is running", controller.getStatus());
    }

    @Test
    void corsFilterRegistration_ConfiguredCorrectly() {
        CorsConfig config = new CorsConfig();
        FilterRegistrationBean<CorsFilter> registration = config.corsFilterRegistration();

        assertNotNull(registration);
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE, registration.getOrder());
    }
}
