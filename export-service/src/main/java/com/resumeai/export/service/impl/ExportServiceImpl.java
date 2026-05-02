package com.resumeai.export.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.export.client.ResumeClient;
import com.resumeai.export.client.SectionClient;
import com.resumeai.export.client.TemplateClient;
import com.resumeai.export.dto.ExportResponse;
import com.resumeai.export.dto.NotificationEvent;
import com.resumeai.export.dto.ResumeDTO;
import com.resumeai.export.dto.SectionDTO;
import com.resumeai.export.dto.TemplateDTO;
import com.resumeai.export.entity.ExportJobRecord;
import com.resumeai.export.repository.ExportJobRepository;
import com.resumeai.export.service.ExportService;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ResumeClient resumeClient;
    private final SectionClient sectionClient;
    private final TemplateClient templateClient;
    private final RabbitTemplate rabbitTemplate;
    private final ExportJobRepository exportJobRepository;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.export}")
    private String exportRoutingKey;

    @Value("${export.output-dir:./exports}")
    private String outputDir;

    @Override
    public ExportResponse exportResume(Long userId, Long resumeId, String requestedFormat) {
        String format = normalizeFormat(requestedFormat);

        ExportJobRecord job = exportJobRepository.save(ExportJobRecord.builder()
                .userId(userId)
                .resumeId(resumeId)
                .format(format)
                .status("PROCESSING")
                .build());

        try {
            ResumeDTO resume = resumeClient.getResumeById(resumeId, userId);
            if (resume == null) {
                return markFailed(job, "Resume not found");
            }

            List<SectionDTO> sections = null;
            try {
                sections = sectionClient.getSectionsByResumeId(resumeId, userId);
            } catch (Exception e) {
                log.warn("Failed to fetch legacy sections or no sections found: {}", e.getMessage());
            }
            
            boolean hasSections = sections != null && !sections.isEmpty();
            boolean hasDynamicSections = resume.getSectionsJson() != null && !resume.getSectionsJson().trim().isEmpty();

            if (!hasSections && !hasDynamicSections) {
                return markFailed(job, "Add at least one section before export");
            }

            TemplateDTO template = resume.getTemplateId() != null
                    ? templateClient.getTemplateById(resume.getTemplateId())
                    : null;

            String payload = "JSON".equals(format)
                    ? buildJsonPayload(resume, sections, template)
                    : buildHtmlPayload(resume, sections, template);
            String filePath = saveToFile(resumeId, format, payload);

            job.setStatus("COMPLETED");
            job.setFilePath(filePath);
            job.setFileSizeKb(resolveFileSizeKb(filePath));
            job.setCompletedAt(LocalDateTime.now());
            job.setExpiresAt(LocalDateTime.now().plusDays(7));
            exportJobRepository.save(job);

            publishNotification(job);
            return toResponse(job, "Resume exported successfully");
        } catch (Exception ex) {
            log.error("Export failed for resumeId={}: {}", resumeId, ex.getMessage(), ex);
            return markFailed(job, "Export failed: " + ex.getMessage());
        }
    }

    @Override
    public ExportResponse getStatus(UUID jobId) {
        ExportJobRecord job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Export job not found"));
        return toResponse(job, "OK");
    }

    @Override
    public List<ExportResponse> getByUser(Long userId) {
        return exportJobRepository.findByUserIdOrderByRequestedAtDesc(userId).stream()
                .map(job -> toResponse(job, "OK"))
                .toList();
    }

    @Override
    public Map<String, Object> getStats(Long userId) {
        long total = exportJobRepository.countByUserId(userId);
        long completed = exportJobRepository.countByUserIdAndStatus(userId, "COMPLETED");
        long failed = exportJobRepository.countByUserIdAndStatus(userId, "FAILED");
        long processing = exportJobRepository.countByUserIdAndStatus(userId, "PROCESSING");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userId", userId);
        stats.put("totalExports", total);
        stats.put("completedExports", completed);
        stats.put("failedExports", failed);
        stats.put("processingExports", processing);
        return stats;
    }

    @Override
    public void delete(UUID jobId) {
        ExportJobRecord job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Export job not found"));

        deletePhysicalFile(job.getFilePath());
        exportJobRepository.deleteById(jobId);
    }

    private ExportResponse markFailed(ExportJobRecord job, String message) {
        job.setStatus("FAILED");
        job.setCompletedAt(LocalDateTime.now());
        exportJobRepository.save(job);
        return toResponse(job, message);
    }

    private ExportResponse toResponse(ExportJobRecord job, String message) {
        return ExportResponse.builder()
                .jobId(job.getJobId())
                .userId(job.getUserId())
                .resumeId(job.getResumeId())
                .format(job.getFormat())
                .status(job.getStatus())
                .filePath(job.getFilePath())
                .fileUrl(job.getJobId() == null ? null : "/api/v1/export/file/" + job.getJobId())
                .fileSizeKb(job.getFileSizeKb())
                .requestedAt(job.getRequestedAt())
                .completedAt(job.getCompletedAt())
                .expiresAt(job.getExpiresAt())
                .message(message)
                .build();
    }

    private void publishNotification(ExportJobRecord job) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(job.getUserId())
                    .subject("Your Resume Export is Ready")
                    .message("Your export for resume ID " + job.getResumeId() + " is ready.")
                    .build();

            rabbitTemplate.convertAndSend(exchange, exportRoutingKey, event);
        } catch (Exception ex) {
            log.warn("Failed to publish export notification for jobId={}: {}", job.getJobId(), ex.getMessage());
        }
    }

    private String normalizeFormat(String requestedFormat) {
        String value = requestedFormat == null ? "PDF" : requestedFormat.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "DOCX", "JSON", "PDF" -> value;
            default -> "PDF";
        };
    }

    private String buildJsonPayload(ResumeDTO resume, List<SectionDTO> sections, TemplateDTO template) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resume", resume);
        payload.put("sections", sections);
        payload.put("template", template);
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    @SuppressWarnings("unchecked")
    private String buildHtmlPayload(ResumeDTO resume, List<SectionDTO> sections, TemplateDTO template) {
        // ── PRIMARY PATH: Use template htmlLayout + resume sectionsJson ──
        // This matches exactly what the builder live preview shows
        if (template != null && template.getHtmlContent() != null && !template.getHtmlContent().isBlank()
                && resume.getSectionsJson() != null && !resume.getSectionsJson().isBlank()) {
            try {
                Map<String, String> data = OBJECT_MAPPER.readValue(resume.getSectionsJson(), Map.class);
                String html = template.getHtmlContent();

                // Replace all {{variable}} placeholders with actual user data
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    String placeholder = "\\{\\{" + entry.getKey() + "\\}\\}";
                    String value = entry.getValue() != null ? entry.getValue().replace("\n", "<br/>") : "";
                    html = html.replaceAll(placeholder, value);
                }

                // Replace [variable] placeholders
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    String placeholder = "\\[" + entry.getKey() + "\\]";
                    String value = entry.getValue() != null ? entry.getValue().replace("\n", "<br/>") : "";
                    html = html.replaceAll(placeholder, value);
                }

                // Replace hardcoded static names
                String name = data.get("name") != null ? data.get("name") : "[name]";
                String title = data.get("title") != null ? data.get("title") : "[title]";
                String email = data.get("email") != null ? data.get("email") : "[email]";
                String phone = data.get("phone") != null ? data.get("phone") : "[phone]";
                String location = data.get("location") != null ? data.get("location") : "[location]";
                String linkedin = data.get("linkedin") != null ? data.get("linkedin") : "[linkedin]";
                String summary = data.get("summary") != null ? data.get("summary") : "";
                String skills = data.get("skills") != null ? data.get("skills") : "";
                String experience = data.get("experience") != null ? data.get("experience") : "";
                String education = data.get("education") != null ? data.get("education") : "";

                html = html.replace("Alexandra Reeves", name)
                           .replace("Marcus Thornton", name)
                           .replace("Sarah Chen", name)
                           .replace("Jane Doe", name)
                           .replace("John Doe", name)
                           .replace("Senior UX Designer", title)
                           .replace("Product Manager", title)
                           .replace("Senior Software Engineer", title)
                           .replace("Marketing Director", title)
                           .replace("alex@email.com", email)
                           .replace("alexandra.reeves@email.com", email)
                           .replace("marcus.t@email.com", email)
                           .replace("sarah.chen@email.com", email)
                           .replace("+1 (555) 204-8821", phone)
                           .replace("+1 (555) 123-4567", phone)
                           .replace("(555) 123-4567", phone)
                           .replace("San Francisco, CA", location)
                           .replace("San Francisco", location)
                           .replace("New York, NY", location)
                           .replace("Seattle, WA", location)
                           .replace("linkedin.com/in/alex", linkedin)
                           .replace("linkedin.com/in/alexandra-reeves", linkedin)
                           .replace("linkedin.com/in/marcust", linkedin)
                           .replace("linkedin.com/in/sarahchen", linkedin);

                // Summary Replacements
                if (!summary.isEmpty()) {
                    html = html.replaceAll("Product design leader with 8\\+ years crafting intuitive digital experiences\\. Specializing in design systems, user research, and cross-functional collaboration\\.", summary)
                               .replaceAll("Results-driven product manager with 5\\+ years of experience leading cross-functional teams to deliver scalable consumer products\\.", summary)
                               .replaceAll("Detail-oriented software engineer with expertise in full-stack development, cloud architecture, and building scalable microservices\\.", summary)
                               .replaceAll("(?i)Results-driven product manager with 9 years[\\s\\S]*?Wharton\\.", summary);
                }

                // Skills Replacements
                if (!skills.isEmpty()) {
                    html = html.replaceAll("Figma / Sketch[\\s\\S]*?HTML/CSS/JS", skills)
                               .replaceAll("Product Strategy[\\s\\S]*?A/B Testing", skills)
                               .replaceAll("Java / Spring Boot[\\s\\S]*?Docker / Kubernetes", skills)
                               .replace("Figma / Sketch", skills)
                               .replaceAll("<div class=\"hard-skill\">Product Strategy[\\s\\S]*?A/B Testing</div>", skills)
                               .replaceAll("(?i)Product Strategy[\\s\\S]*?A/B Testing</div></div>", skills);
                }

                // Experience Replacements
                if (!experience.isEmpty()) {
                    html = html.replaceAll("Lead UX Designer[\\s\\S]*?Conducted 120\\+ user interviews across 8 countries", experience)
                               .replaceAll("Senior Product Manager[\\s\\S]*?Grew user retention by 25%", experience)
                               .replaceAll("Senior Backend Engineer[\\s\\S]*?Mentored 3 junior developers", experience)
                               .replaceAll("Lead UX Designer[\\s\\S]*?weekly design critiques", experience)
                               .replaceAll("(?i)Owned <strong>Google Discover</strong>[\\s\\S]*?incremental ARR in FY2023</li>", experience);
                }

                // Education Replacements
                if (!education.isEmpty()) {
                    html = html.replaceAll("B\\.[F]?\\.?[S]?\\.? Interaction Design[\\s\\S]*?College of the Arts", education)
                               .replaceAll("B\\.S\\. Interaction Design[\\s\\S]*?Stanford University", education)
                               .replaceAll("M\\.B\\.A\\. Product Management[\\s\\S]*?Harvard Business School", education)
                               .replaceAll("M\\.S\\. Computer Science[\\s\\S]*?MIT", education)
                               .replaceAll("(?i)MBA, Product Strategy[\\s\\S]*?Silver Medal", education);
                }

                // Remove any remaining unreplaced placeholders
                html = html.replaceAll("\\{\\{\\w+\\}\\}", "");

                String css = template.getCssContent() != null ? template.getCssContent() : defaultCss();
                return "<html><head><style>" + css + "</style></head><body>" + html + "</body></html>";
            } catch (Exception ex) {
                log.warn("Failed to render template with sectionsJson, falling back to legacy: {}", ex.getMessage());
            }
        }

        // ── FALLBACK: Build HTML from individual sections (legacy) ──
        StringBuilder body = new StringBuilder();
        body.append("<div class=\"resume-header\">")
                .append("<h1>").append(safe(resume.getTitle())).append("</h1>")
                .append("<p class=\"target-role\">").append(safe(resume.getTargetJobTitle())).append("</p>")
                .append("</div>");

        if (sections != null) {
            sections.stream()
                    .filter(s -> !Boolean.FALSE.equals(s.getIsVisible()))
                    .sorted((a, b) -> Integer.compare(a.getOrderIndex() == null ? 0 : a.getOrderIndex(),
                            b.getOrderIndex() == null ? 0 : b.getOrderIndex()))
                    .forEach(section -> body.append("<div class=\"section\">")
                            .append("<h2>").append(safe(section.getTitle() != null
                                    ? section.getTitle()
                                    : section.getSectionType())).append("</h2>")
                            .append("<div>").append(safe(section.getContent())).append("</div>")
                            .append("</div>"));
        }

        String css = template != null && template.getCssContent() != null ? template.getCssContent() : defaultCss();
        return "<html><head><style>" + css + "</style></head><body>" + body + "</body></html>";
    }

    private String defaultCss() {
        return "body{font-family:Arial,sans-serif;margin:40px;color:#333;}"
                + ".resume-header{border-bottom:2px solid #333;padding-bottom:10px;margin-bottom:20px;}"
                + ".resume-header h1{margin:0;font-size:28px;}"
                + ".section{margin-bottom:20px;}"
                + ".section h2{font-size:18px;border-bottom:1px solid #ccc;padding-bottom:4px;}";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String saveToFile(Long resumeId, String format, String content) throws Exception {
        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);

        String extension = "JSON".equals(format) ? ".json" : ("PDF".equals(format) ? ".pdf" : ".html");
        String fileName = "resume_" + resumeId + "_" + LocalDateTime.now().format(FILE_TS)
                + "_" + format.toLowerCase(Locale.ROOT) + extension;

        Path filePath = dir.resolve(fileName);

        if ("PDF".equals(format)) {
            try (FileOutputStream os = new FileOutputStream(filePath.toFile())) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withHtmlContent(content, null);
                builder.toStream(os);
                builder.run();
            }
        } else {
            Files.writeString(filePath, content);
        }

        return filePath.toAbsolutePath().toString();
    }

    private long resolveFileSizeKb(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return 0;
        }
        try {
            long bytes = Files.size(Path.of(filePath));
            return Math.max(1, bytes / 1024);
        } catch (IOException ex) {
            return 0;
        }
    }

    private void deletePhysicalFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ex) {
            log.warn("Could not delete export file {}: {}", filePath, ex.getMessage());
        }
    }
}
