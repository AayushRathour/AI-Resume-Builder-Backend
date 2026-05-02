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

@SpringBootApplication
@EnableDiscoveryClient
public class TemplateServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceApplication.class);

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
                if (html != null && !html.contains("{{name}}")) {
                    html = html.replace("Alexandra Reeves", "{{name}}")
                               .replace("Marcus Thornton", "{{name}}")
                               .replace("Sarah Chen", "{{name}}")
                               .replace("Jane Doe", "{{name}}")
                               .replace("John Doe", "{{name}}")
                               .replace("Priya Krishnamurthy", "{{name}}")
                               .replace("Emily Chen", "{{name}}")
                               .replace("David Kim", "{{name}}")
                               .replace("Alex Rivera", "{{name}}");
                               
                    html = html.replace("Senior Software Engineer", "{{title}}")
                               .replace("Product Manager", "{{title}}")
                               .replace("Marketing Director", "{{title}}")
                               .replace("Growth & Analytics", "{{title}}")
                               .replace("Senior UI/UX Designer", "{{title}}")
                               .replace("alexandra.reeves@email.com", "{{email}}")
                               .replace("marcus.t@email.com", "{{email}}")
                               .replace("sarah.chen@email.com", "{{email}}")
                               .replace("priya@pm.io", "{{email}}")
                               .replace("emily.chen@design.co", "{{email}}")
                               .replace("+1 (555) 123-4567", "{{phone}}")
                               .replace("(555) 123-4567", "{{phone}}")
                               .replace("+1 234 567 8900", "{{phone}}")
                               .replace("+1 (650) 334-9901", "{{phone}}")
                               .replace("+1 (415) 555-0198", "{{phone}}")
                               .replace("San Francisco, CA", "{{location}}")
                               .replace("New York, NY", "{{location}}")
                               .replace("Seattle, WA", "{{location}}")
                               .replace("San Jose, CA", "{{location}}")
                               .replace("Austin, TX", "{{location}}")
                               .replace("linkedin.com/in/alexandra-reeves", "{{linkedin}}")
                               .replace("linkedin.com/in/marcust", "{{linkedin}}")
                               .replace("linkedin.com/in/sarahchen", "{{linkedin}}")
                               .replace("linkedin.com/in/priyak", "{{linkedin}}")
                               .replace("linkedin.com/in/emilyc", "{{linkedin}}");
                               
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
