package com.resumeai.template;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import com.resumeai.template.repository.TemplateRepository;
import com.resumeai.template.entity.Template;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Application entry point for template-service. */

@SpringBootApplication
@EnableDiscoveryClient
public class TemplateServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceApplication.class);

    private static final String PLACEHOLDER_NAME = "{{name}}";
    private static final String PLACEHOLDER_TITLE = "{{title}}";
    private static final String PLACEHOLDER_EMAIL = "{{email}}";
    private static final String PLACEHOLDER_PHONE = "{{phone}}";
    private static final String PLACEHOLDER_LOCATION = "{{location}}";
    private static final String PLACEHOLDER_LINKEDIN = "{{linkedin}}";

	public static void main(String[] args) {
		SpringApplication.run(TemplateServiceApplication.class, args);
	}

    @Bean
    public CommandLineRunner migrateTemplates(TemplateRepository templateRepository) {
        return args -> {
            log.info("Starting template migration...");
            List<Template> templates = templateRepository.findAll();
            for (Template t : templates) {
                boolean changed = false;
                
                // Update fieldsJson if needed
                if (t.getFieldsJson() == null || t.getFieldsJson().isBlank() || t.getFieldsJson().equals("[]") || t.getFieldsJson().length() < 100) {
                    t.setFieldsJson("[{\"key\":\"name\",\"label\":\"Full Name\"},{\"key\":\"title\",\"label\":\"Job Title\"},{\"key\":\"email\",\"label\":\"Email\"},{\"key\":\"phone\",\"label\":\"Phone Number\"},{\"key\":\"location\",\"label\":\"Location\"},{\"key\":\"linkedin\",\"label\":\"LinkedIn Profile\"},{\"key\":\"github\",\"label\":\"GitHub Profile\"},{\"key\":\"summary\",\"label\":\"Professional Summary\"},{\"key\":\"skills\",\"label\":\"Skills\"},{\"key\":\"experience\",\"label\":\"Work Experience\"},{\"key\":\"education\",\"label\":\"Education\"}]");
                    changed = true;
                }
                
                // Update HTML placeholders
                String html = t.getHtmlContent();
                if (html != null && !html.contains(PLACEHOLDER_NAME)) {
                    html = html.replace("Alexandra Reeves", PLACEHOLDER_NAME)
                               .replace("Marcus Thornton", PLACEHOLDER_NAME)
                               .replace("Sarah Chen", PLACEHOLDER_NAME)
                               .replace("Jane Doe", PLACEHOLDER_NAME)
                               .replace("John Doe", PLACEHOLDER_NAME)
                               .replace("Priya Krishnamurthy", PLACEHOLDER_NAME)
                               .replace("Emily Chen", PLACEHOLDER_NAME)
                               .replace("David Kim", PLACEHOLDER_NAME)
                               .replace("Alex Rivera", PLACEHOLDER_NAME);
                               
                    html = html.replace("Senior Software Engineer", PLACEHOLDER_TITLE)
                               .replace("Product Manager", PLACEHOLDER_TITLE)
                               .replace("Marketing Director", PLACEHOLDER_TITLE)
                               .replace("Growth & Analytics", PLACEHOLDER_TITLE)
                               .replace("Senior UI/UX Designer", PLACEHOLDER_TITLE)
                               .replace("alexandra.reeves@email.com", PLACEHOLDER_EMAIL)
                               .replace("marcus.t@email.com", PLACEHOLDER_EMAIL)
                               .replace("sarah.chen@email.com", PLACEHOLDER_EMAIL)
                               .replace("priya@pm.io", PLACEHOLDER_EMAIL)
                               .replace("emily.chen@design.co", PLACEHOLDER_EMAIL)
                               .replace("+1 (555) 123-4567", PLACEHOLDER_PHONE)
                               .replace("(555) 123-4567", PLACEHOLDER_PHONE)
                               .replace("+1 234 567 8900", PLACEHOLDER_PHONE)
                               .replace("+1 (650) 334-9901", PLACEHOLDER_PHONE)
                               .replace("+1 (415) 555-0198", PLACEHOLDER_PHONE)
                               .replace("San Francisco, CA", PLACEHOLDER_LOCATION)
                               .replace("New York, NY", PLACEHOLDER_LOCATION)
                               .replace("Seattle, WA", PLACEHOLDER_LOCATION)
                               .replace("San Jose, CA", PLACEHOLDER_LOCATION)
                               .replace("Austin, TX", PLACEHOLDER_LOCATION)
                               .replace("linkedin.com/in/alexandra-reeves", PLACEHOLDER_LINKEDIN)
                               .replace("linkedin.com/in/marcust", PLACEHOLDER_LINKEDIN)
                               .replace("linkedin.com/in/sarahchen", PLACEHOLDER_LINKEDIN)
                               .replace("linkedin.com/in/priyak", PLACEHOLDER_LINKEDIN)
                               .replace("linkedin.com/in/emilyc", PLACEHOLDER_LINKEDIN);
                               
                    // For text areas, we need to replace larger chunks or just let users replace the whole section
                    // We can at least wrap the main sections if they exist, but it's safer to just replace known text.
                    t.setHtmlContent(html);
                    changed = true;
                }
                
                if (changed) {
                    templateRepository.save(t);
                    log.info("Migrated template: {}", t.getName());
                }
            }
            log.info("Template migration complete.");
        };
    }
}



