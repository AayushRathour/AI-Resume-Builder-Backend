package com.resumeai.section.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Applies startup schema compatibility checks for legacy section records.
 * Keeps section-service upgrades safe without forcing destructive migrations.
 */

@Configuration
public class SchemaCompatibilityInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityInitializer.class);

    @Bean
    public ApplicationRunner sectionSchemaCompatibilityRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Keep existing data intact and only relax legacy enum restriction.
                jdbcTemplate.execute("ALTER TABLE sections MODIFY COLUMN section_type VARCHAR(64) NOT NULL");
                log.info("Ensured sections.section_type accepts all current section enum values");
            } catch (Exception ex) {
                log.warn("Skipping schema compatibility alteration for sections.section_type: {}", ex.getMessage());
            }
        };
    }
}
