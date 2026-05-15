package com.resumeai.section.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** OpenAPI configuration for service documentation in section-service. */

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sectionServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resume Section Service API")
                        .version("v1")
                        .description("Structured section operations for ResumeAI"));
    }
}
