package com.resumeai.resume.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI configuration for resume-service endpoints.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Defines the OpenAPI document metadata.
     */
    @Bean
    public OpenAPI resumeServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resume Service API")
                        .version("v1")
                        .description("Resume container operations for ResumeAI"));
    }
}
