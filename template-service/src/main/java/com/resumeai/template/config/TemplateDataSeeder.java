package com.resumeai.template.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.resumeai.template.entity.Template;
import com.resumeai.template.entity.TemplateCategory;
import com.resumeai.template.repository.TemplateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TemplateDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TemplateDataSeeder.class);

    private final TemplateRepository templateRepository;

    @Override
    public void run(String... args) {

        // ── Step 1: Ensure all existing templates are active ──────────────────
        List<Template> allTemplates = templateRepository.findAll();
        int activated = 0;
        for (Template t : allTemplates) {
            boolean changed = false;
            if (!t.isActive()) {
                t.setActive(true);
                changed = true;
            }
            if (t.getCategory() == null) {
                t.setCategory(TemplateCategory.PROFESSIONAL);
                changed = true;
            }
            if (t.getUsageCount() == null) {
                t.setUsageCount(0L);
                changed = true;
            }
            if (changed) {
                templateRepository.save(t);
                activated++;
            }
        }
        if (activated > 0) {
            log.info("Activated/repaired {} existing templates", activated);
        }

        // ── Step 2: Seed default templates if not already present ─────────────
        List<Template> defaultTemplates = List.of(
                Template.builder()
                        .name("Professional Clean")
                        .category(TemplateCategory.PROFESSIONAL)
                        .description("Simple and professional layout for most job roles")
                        .htmlContent("<div><h1>{{fullName}}</h1><p>{{summary}}</p></div>")
                        .cssContent("body { font-family: Arial; }")
                        .isPremium(false)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),
                Template.builder()
                        .name("Modern Blue")
                        .category(TemplateCategory.MODERN)
                        .description("Modern style with clean blue accents")
                        .htmlContent("<div><h1>{{fullName}}</h1><h2>{{targetJobTitle}}</h2><p>{{summary}}</p></div>")
                        .cssContent("body { font-family: Arial; color: #1e3a8a; }")
                        .isPremium(false)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),
                Template.builder()
                        .name("Creative Dark")
                        .category(TemplateCategory.CREATIVE)
                        .description("Bold dark profile for creative professionals")
                        .htmlContent("<div><h1>{{fullName}}</h1><section>{{summary}}</section></div>")
                        .cssContent("body { font-family: Arial; background: #0f172a; color: #e2e8f0; }")
                        .isPremium(true)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),
                Template.builder()
                        .name("ATS Optimized")
                        .category(TemplateCategory.ATS_OPTIMISED)
                        .description("ATS-friendly format focused on parsing compatibility")
                        .htmlContent("<div><h1>{{fullName}}</h1><p>{{summary}}</p><p>{{skills}}</p></div>")
                        .cssContent("body { font-family: Arial; line-height: 1.4; }")
                        .isPremium(false)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),
                Template.builder()
                        .name("Minimal Elegant")
                        .category(TemplateCategory.MINIMALIST)
                        .description("Minimal layout with elegant spacing")
                        .htmlContent("<div><h1>{{fullName}}</h1><p>{{summary}}</p><p>{{experience}}</p></div>")
                        .cssContent("body { font-family: Arial; color: #111827; }")
                        .isPremium(true)
                        .isActive(true)
                        .usageCount(0L)
                        .build());

        int inserted = 0;
        for (Template template : defaultTemplates) {
            if (!templateRepository.existsByName(template.getName())) {
                templateRepository.save(template);
                inserted++;
            }
        }

        long totalActive = templateRepository.countByIsActiveTrue();
        if (inserted > 0) {
            log.info("Seeded {} new default templates. Total active templates: {}", inserted, totalActive);
        } else {
            log.info("Template seeder: {} active templates already in DB — no seeding needed", totalActive);
        }
    }
}
