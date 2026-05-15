package com.resumeai.api_gateway.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Minimal endpoint to ensure Swagger/OpenAPI initializes at the gateway.
 */
@RestController
@Tag(name = "Gateway Info", description = "API Gateway Information")
public class SwaggerDummyController {

    /**
     * Health-like endpoint for gateway status checks.
     */
    @Operation(summary = "Get Gateway Status", description = "Dummy endpoint to force SpringDoc initialization in Gateway")
    @GetMapping("/api/v1/gateway/status")
    public String getStatus() {
        return "Gateway is running";
    }
}
