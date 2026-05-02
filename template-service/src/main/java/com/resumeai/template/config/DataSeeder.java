package com.resumeai.template.config;

import com.resumeai.template.entity.Template;
import com.resumeai.template.entity.TemplateCategory;
import com.resumeai.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the template_db with default resume templates if none exist.
 * Runs once on startup — skips if templates already present.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final TemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        if (templateRepository.count() > 0) {
            log.info("Templates already seeded ({} found). Skipping.", templateRepository.count());
            return;
        }

        log.info("No templates found. Seeding default templates...");

        List<Template> defaults = List.of(
                Template.builder()
                        .name("Classic Professional")
                        .category(TemplateCategory.PROFESSIONAL)
                        .description("A clean, traditional resume layout ideal for corporate and business roles.")
                        .htmlContent(professionalHtml())
                        .cssContent(professionalCss())
                        .isPremium(false)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),

                Template.builder()
                        .name("Modern Minimal")
                        .category(TemplateCategory.MODERN)
                        .description("A sleek, modern design with clean typography and subtle accents.")
                        .htmlContent(modernHtml())
                        .cssContent(modernCss())
                        .isPremium(false)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),

                Template.builder()
                        .name("Creative Designer")
                        .category(TemplateCategory.CREATIVE)
                        .description("A bold, colorful template perfect for designers and creative professionals.")
                        .htmlContent(creativeHtml())
                        .cssContent(creativeCss())
                        .isPremium(true)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),

                Template.builder()
                        .name("ATS Optimised")
                        .category(TemplateCategory.ATS_OPTIMISED)
                        .description("Specifically designed to pass Applicant Tracking Systems with maximum compatibility.")
                        .htmlContent(atsHtml())
                        .cssContent(atsCss())
                        .isPremium(false)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),

                Template.builder()
                        .name("Executive Premium")
                        .category(TemplateCategory.PROFESSIONAL)
                        .description("An elegant, premium layout for senior executives and leadership roles.")
                        .htmlContent(executiveHtml())
                        .cssContent(executiveCss())
                        .isPremium(true)
                        .isActive(true)
                        .usageCount(0L)
                        .build(),

                Template.builder()
                        .name("Clean Minimalist")
                        .category(TemplateCategory.MINIMALIST)
                        .description("A distraction-free, whitespace-rich design that lets your content shine.")
                        .htmlContent(minimalistHtml())
                        .cssContent(minimalistCss())
                        .isPremium(false)
                        .isActive(true)
                        .usageCount(0L)
                        .build()
        );

        templateRepository.saveAll(defaults);
        log.info("Seeded {} default templates successfully.", defaults.size());
    }

    // ── Template HTML/CSS Generators ──────────────────────────────────────────

    private String professionalHtml() {
        return """
                <div class="resume professional">
                  <header>
                    <h1>{{name}}</h1>
                    <p class="job-title">{{title}}</p>
                    <div class="contact-info">
                      <span>{{email}}</span> | <span>{{phone}}</span>
                    </div>
                  </header>
                  <section class="summary">
                    <h2>Professional Summary</h2>
                    <p>{{summary}}</p>
                  </section>
                  <section class="experience">
                    <h2>Work Experience</h2>
                    <p>{{experience}}</p>
                  </section>
                  <section class="education">
                    <h2>Education</h2>
                    <p>{{education}}</p>
                  </section>
                  <section class="skills">
                    <h2>Skills</h2>
                    <p>{{skills}}</p>
                  </section>
                </div>
                """;
    }

    private String professionalCss() {
        return """
                .resume.professional { font-family: 'Georgia', serif; max-width: 800px; margin: 0 auto; padding: 40px; color: #2d3748; }
                .resume.professional header { text-align: center; border-bottom: 3px solid #2d3748; padding-bottom: 20px; margin-bottom: 24px; }
                .resume.professional h1 { font-size: 32px; margin: 0 0 4px; color: #1a202c; letter-spacing: 2px; text-transform: uppercase; }
                .resume.professional .job-title { font-size: 16px; color: #4a5568; font-style: italic; margin: 0 0 8px; }
                .resume.professional .contact-info { font-size: 13px; color: #718096; }
                .resume.professional h2 { font-size: 16px; text-transform: uppercase; letter-spacing: 1.5px; border-bottom: 1px solid #cbd5e0; padding-bottom: 6px; margin: 20px 0 10px; color: #2d3748; }
                .resume.professional section p { font-size: 14px; line-height: 1.7; color: #4a5568; }
                """;
    }

    private String modernHtml() {
        return """
                <div class="resume modern">
                  <div class="header-bar">
                    <h1>{{name}}</h1>
                    <p class="subtitle">{{title}}</p>
                  </div>
                  <div class="contact-strip">
                    <span>✉ {{email}}</span>
                    <span>📱 {{phone}}</span>
                  </div>
                  <div class="content">
                    <section><h2>About</h2><p>{{summary}}</p></section>
                    <section><h2>Experience</h2><p>{{experience}}</p></section>
                    <section><h2>Education</h2><p>{{education}}</p></section>
                    <section><h2>Skills</h2><p>{{skills}}</p></section>
                  </div>
                </div>
                """;
    }

    private String modernCss() {
        return """
                .resume.modern { font-family: 'Segoe UI', sans-serif; max-width: 800px; margin: 0 auto; }
                .resume.modern .header-bar { background: linear-gradient(135deg, #667eea, #764ba2); padding: 32px 40px; color: white; }
                .resume.modern h1 { margin: 0; font-size: 28px; font-weight: 700; }
                .resume.modern .subtitle { margin: 4px 0 0; opacity: 0.9; font-size: 15px; }
                .resume.modern .contact-strip { background: #f7fafc; padding: 10px 40px; font-size: 13px; color: #4a5568; display: flex; gap: 24px; border-bottom: 1px solid #e2e8f0; }
                .resume.modern .content { padding: 24px 40px; }
                .resume.modern h2 { font-size: 15px; text-transform: uppercase; letter-spacing: 1px; color: #667eea; border-bottom: 2px solid #667eea; padding-bottom: 4px; margin: 20px 0 10px; }
                .resume.modern section p { font-size: 14px; line-height: 1.6; color: #4a5568; }
                """;
    }

    private String creativeHtml() {
        return """
                <div class="resume creative">
                  <aside class="sidebar">
                    <div class="avatar">{{name}}</div>
                    <div class="sidebar-section">
                      <h3>Contact</h3>
                      <p>{{email}}</p>
                      <p>{{phone}}</p>
                    </div>
                    <div class="sidebar-section">
                      <h3>Skills</h3>
                      <p>{{skills}}</p>
                    </div>
                  </aside>
                  <main class="main-content">
                    <h1>{{name}}</h1>
                    <p class="role">{{title}}</p>
                    <section><h2>Profile</h2><p>{{summary}}</p></section>
                    <section><h2>Experience</h2><p>{{experience}}</p></section>
                    <section><h2>Education</h2><p>{{education}}</p></section>
                  </main>
                </div>
                """;
    }

    private String creativeCss() {
        return """
                .resume.creative { font-family: 'Segoe UI', sans-serif; display: flex; max-width: 800px; margin: 0 auto; min-height: 500px; }
                .resume.creative .sidebar { width: 220px; background: #1a202c; color: #e2e8f0; padding: 32px 20px; flex-shrink: 0; }
                .resume.creative .avatar { width: 80px; height: 80px; background: #f6ad55; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 20px; color: #1a202c; margin: 0 auto 20px; text-align: center; overflow: hidden; }
                .resume.creative .sidebar-section { margin-bottom: 20px; }
                .resume.creative .sidebar h3 { font-size: 12px; text-transform: uppercase; letter-spacing: 1.5px; color: #f6ad55; margin: 0 0 8px; }
                .resume.creative .sidebar p { font-size: 12px; line-height: 1.5; margin: 2px 0; }
                .resume.creative .main-content { flex: 1; padding: 32px; }
                .resume.creative h1 { margin: 0; font-size: 28px; color: #1a202c; }
                .resume.creative .role { color: #f6ad55; font-size: 15px; margin: 4px 0 20px; font-weight: 600; }
                .resume.creative h2 { font-size: 14px; text-transform: uppercase; letter-spacing: 1px; color: #1a202c; border-left: 3px solid #f6ad55; padding-left: 10px; margin: 20px 0 10px; }
                .resume.creative section p { font-size: 13px; line-height: 1.6; color: #4a5568; }
                """;
    }

    private String atsHtml() {
        return """
                <div class="resume ats">
                  <h1>{{name}}</h1>
                  <p class="meta">{{title}} | {{email}} | {{phone}}</p>
                  <hr/>
                  <h2>PROFESSIONAL SUMMARY</h2>
                  <p>{{summary}}</p>
                  <h2>WORK EXPERIENCE</h2>
                  <p>{{experience}}</p>
                  <h2>EDUCATION</h2>
                  <p>{{education}}</p>
                  <h2>SKILLS</h2>
                  <p>{{skills}}</p>
                </div>
                """;
    }

    private String atsCss() {
        return """
                .resume.ats { font-family: Arial, Helvetica, sans-serif; max-width: 800px; margin: 0 auto; padding: 40px; color: #000; }
                .resume.ats h1 { font-size: 24px; margin: 0 0 4px; }
                .resume.ats .meta { font-size: 13px; color: #333; margin: 0 0 16px; }
                .resume.ats hr { border: none; border-top: 2px solid #000; margin: 0 0 16px; }
                .resume.ats h2 { font-size: 14px; text-transform: uppercase; margin: 20px 0 6px; border-bottom: 1px solid #999; padding-bottom: 3px; }
                .resume.ats p { font-size: 13px; line-height: 1.6; }
                """;
    }

    private String executiveHtml() {
        return """
                <div class="resume executive">
                  <header>
                    <h1>{{name}}</h1>
                    <div class="title-line">{{title}}</div>
                    <div class="contact">{{email}} · {{phone}}</div>
                  </header>
                  <section><h2>Executive Summary</h2><p>{{summary}}</p></section>
                  <section><h2>Professional Experience</h2><p>{{experience}}</p></section>
                  <section><h2>Education & Credentials</h2><p>{{education}}</p></section>
                  <section><h2>Core Competencies</h2><p>{{skills}}</p></section>
                </div>
                """;
    }

    private String executiveCss() {
        return """
                .resume.executive { font-family: 'Palatino Linotype', 'Book Antiqua', serif; max-width: 800px; margin: 0 auto; padding: 48px; color: #1a1a2e; }
                .resume.executive header { text-align: center; margin-bottom: 32px; }
                .resume.executive h1 { font-size: 36px; margin: 0; color: #16213e; letter-spacing: 3px; }
                .resume.executive .title-line { font-size: 15px; color: #e94560; font-weight: 600; margin: 6px 0; text-transform: uppercase; letter-spacing: 2px; }
                .resume.executive .contact { font-size: 12px; color: #666; }
                .resume.executive h2 { font-size: 14px; text-transform: uppercase; letter-spacing: 2px; color: #16213e; border-bottom: 2px solid #e94560; padding-bottom: 6px; margin: 28px 0 12px; }
                .resume.executive section p { font-size: 14px; line-height: 1.8; color: #333; }
                """;
    }

    private String minimalistHtml() {
        return """
                <div class="resume minimalist">
                  <h1>{{name}}</h1>
                  <p class="tagline">{{title}}</p>
                  <p class="contact">{{email}} · {{phone}}</p>
                  <section><h2>Summary</h2><p>{{summary}}</p></section>
                  <section><h2>Experience</h2><p>{{experience}}</p></section>
                  <section><h2>Education</h2><p>{{education}}</p></section>
                  <section><h2>Skills</h2><p>{{skills}}</p></section>
                </div>
                """;
    }

    private String minimalistCss() {
        return """
                .resume.minimalist { font-family: 'Helvetica Neue', Arial, sans-serif; max-width: 700px; margin: 0 auto; padding: 48px; color: #333; }
                .resume.minimalist h1 { font-size: 28px; font-weight: 300; margin: 0; color: #111; }
                .resume.minimalist .tagline { font-size: 14px; color: #888; margin: 4px 0 2px; }
                .resume.minimalist .contact { font-size: 12px; color: #aaa; margin: 0 0 32px; }
                .resume.minimalist h2 { font-size: 11px; text-transform: uppercase; letter-spacing: 3px; color: #999; margin: 28px 0 8px; }
                .resume.minimalist section p { font-size: 14px; line-height: 1.7; color: #444; }
                """;
    }
}
