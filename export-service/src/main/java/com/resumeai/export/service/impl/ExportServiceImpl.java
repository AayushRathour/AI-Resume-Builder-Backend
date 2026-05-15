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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Implements export workflows and service-layer orchestration. */

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

        private static final String DEFAULT_EMPTY_TEMPLATE = """
                <div style=\"font-family:Arial,sans-serif;padding:40px;\">
                    <h1>{{name}}</h1>
                    <p>{{title}}</p>
                    <p>{{summary}}</p>
                    {{skills}}
                    {{experience}}
                    {{education}}
                    {{projects}}
                </div>
                """;

    @Override
    public ExportResponse exportResume(Long userId, Long resumeId, String requestedFormat, Long templateId) {
        // Normalize requested output and create a tracked export job before processing.
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

            try {
                log.info("PDF DATA: {}", OBJECT_MAPPER.writeValueAsString(resume));
            } catch (JsonProcessingException ex) {
                log.debug("PDF DATA serialization failed: {}", ex.getMessage());
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

            // Resolve template priority: explicit request template first, resume-linked template second.
            Long effectiveTemplateId = templateId != null ? templateId : resume.getTemplateId();
            TemplateDTO template = null;
            if (effectiveTemplateId != null) {
                try {
                    template = templateClient.getTemplateById(effectiveTemplateId);
                } catch (Exception ex) {
                    // Keep export functional even when template-service is down or templateId is invalid.
                    log.warn("Template lookup failed for templateId={}, falling back to default template: {}",
                            effectiveTemplateId, ex.getMessage());
                }
            }

            String payload = "JSON".equals(format)
                    ? buildJsonPayload(resume, sections, template)
                    : buildHtmlPayload(resume, sections, template);

            if (!"JSON".equals(format)) {
                log.info("PDF HTML length: {}", payload.length());
                log.debug("PDF HTML: {}", payload);
            }
            String filePath;
            try {
                filePath = saveToFile(resumeId, format, payload);
            } catch (Exception primaryEx) {
                if (!"PDF".equals(format)) {
                    throw primaryEx;
                }
                log.warn("Primary PDF render failed for resumeId={} (templateId={}). Retrying with safe default template. Cause: {}",
                        resumeId, effectiveTemplateId, primaryEx.getMessage());
                String safePayload = buildHtmlPayload(resume, sections, null);
                filePath = saveToFile(resumeId, format, safePayload);
            }

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
        // Publish export.completed so notification-service can alert users asynchronously.
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
        // Primary path: render saved sectionsJson directly into the selected template layout.
        if (resume.getSectionsJson() != null && !resume.getSectionsJson().isBlank()) {
            try {
                String baseTemplate = template != null && template.getHtmlContent() != null && !template.getHtmlContent().isBlank()
                        ? template.getHtmlContent()
                        : DEFAULT_EMPTY_TEMPLATE;
                String html = renderTemplateFromSectionsJson(baseTemplate, resume.getSectionsJson());
                log.info("PDF HTML compiled for resumeId={} (chars={})", resume.getResumeId(), html.length());
                String css = template != null ? template.getCssContent() : null;
                return finalizeHtml(html, css);
            } catch (Exception ex) {
                log.warn("Failed to render template with sectionsJson, falling back to legacy: {}", ex.getMessage());
            }
        }

        // Fallback path: rebuild HTML from legacy section payloads when sectionsJson is not available.
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
        return finalizeHtml(body.toString(), css);
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

    private String renderTemplateFromSectionsJson(String htmlTemplate, String sectionsJson) throws JsonProcessingException {
        Map<String, Object> data = OBJECT_MAPPER.readValue(sectionsJson, Map.class);
        String html = htmlTemplate;

        Map<String, Object> personal = data.get("personal") instanceof Map
                ? (Map<String, Object>) data.get("personal")
                : new LinkedHashMap<>();

        String name = toText(personal.get("name"));
        String title = toText(personal.get("title"));
        String email = toText(personal.get("email"));
        String phone = toText(personal.get("phone"));
        String location = toText(personal.get("location"));
        String linkedin = toText(personal.get("linkedin"));
        String github = toText(personal.get("github"));
        String website = toText(personal.get("website"));

        html = replaceToken(html, "name", toHtmlText(name));
        html = replaceToken(html, "initials", toHtmlText(buildInitials(name)));
        html = replaceToken(html, "title", toHtmlText(title));
        html = replaceToken(html, "email", toHtmlText(email));
        html = replaceToken(html, "phone", toHtmlText(phone));
        html = replaceToken(html, "location", toHtmlText(location));
        html = replaceLinkToken(html, "linkedin", linkedin);
        html = replaceLinkToken(html, "github", github);
        html = replaceLinkToken(html, "website", website);

        html = replaceToken(html, "summary", toHtmlText(toText(data.get("summary"))));
        html = replaceToken(html, "skills", formatSkills(data.get("skills")));
        html = replaceToken(html, "experience", formatExperience(data.get("experience")));
        html = replaceToken(html, "education", formatEducation(data.get("education")));
        html = replaceToken(html, "projects", formatProjects(data.get("projects")));

        html = html.replaceAll("\\{\\{\\s*[a-zA-Z_]+\\s*\\}\\}", "");
        html = html.replaceAll("\\{\\s*[a-zA-Z_]+\\s*\\}", "");
        html = html.replaceAll("\\[\\[\\s*[a-zA-Z_]+\\s*\\]\\]", "");

        return html;
    }

    private String finalizeHtml(String html, String css) {
        if (html == null) {
            html = "";
        }

        String trimmed = html.trim();
        boolean hasHtmlTag = trimmed.toLowerCase(Locale.ROOT).contains("<html");
        boolean hasHeadTag = trimmed.toLowerCase(Locale.ROOT).contains("<head");

        if (css != null && !css.isBlank()) {
            String styleBlock = "<style>" + css + "</style>";
            if (hasHeadTag) {
                return html.replaceFirst("(?i)</head>", styleBlock + "</head>");
            }
            if (hasHtmlTag) {
                return html.replaceFirst("(?i)<html[^>]*>", "$0<head>" + styleBlock + "</head>");
            }
            return "<html><head>" + styleBlock + "</head><body>" + html + "</body></html>";
        }

        if (hasHtmlTag) {
            return html;
        }

        return "<html><body>" + html + "</body></html>";
    }

    private String buildInitials(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String[] parts = trimmed.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                out.append(part.charAt(0));
            }
            if (out.length() >= 2) {
                break;
            }
        }
        return out.toString().toUpperCase(Locale.ROOT);
    }

    private String replaceToken(String html, String key, String value) {
        String safeValue = value == null ? "" : value;
        String pattern = "\\{\\{\\s*" + key + "\\s*\\}\\}|\\{\\s*" + key + "\\s*\\}|\\[\\[\\s*" + key + "\\s*\\]\\]";
        return html.replaceAll(pattern, java.util.regex.Matcher.quoteReplacement(safeValue));
    }

    private String replaceLinkToken(String html, String key, String url) {
        String tokenPattern = "\\{\\{\\s*" + key + "\\s*\\}\\}|\\{\\s*" + key + "\\s*\\}|\\[\\[\\s*" + key + "\\s*\\]\\]";
        String escaped = escapeHtml(url);

        if (escaped.isBlank()) {
            return html.replaceAll(tokenPattern, "");
        }

        Pattern attrPattern = Pattern.compile("(href|src)=([\"']?)\\s*(" + tokenPattern + ")\\s*\\2", Pattern.CASE_INSENSITIVE);
        Matcher matcher = attrPattern.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String quote = matcher.group(2);
            String safeQuote = quote == null || quote.isBlank() ? "\"" : quote;
            String replacement = matcher.group(1) + "=" + safeQuote + Matcher.quoteReplacement(escaped) + safeQuote;
            matcher.appendReplacement(out, replacement);
        }
        matcher.appendTail(out);

        return out.toString().replaceAll(tokenPattern, Matcher.quoteReplacement(escaped));
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toHtmlText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String escaped = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
        return escaped.replace("\n", "<br/>");
    }

    private String escapeHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    private String linkOrText(String value, String label) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String escaped = toHtmlText(value);
        if (value.toLowerCase(Locale.ROOT).startsWith("http")) {
            return "<a href=\"" + escaped + "\" target=\"_blank\">" + toHtmlText(label) + "</a>";
        }
        return escaped;
    }

    private String formatSkills(Object raw) {
        if (!(raw instanceof List<?> skills) || skills.isEmpty()) {
            return "";
        }
        String items = skills.stream()
                .map(this::toText)
                .filter(s -> !s.isBlank())
                .map(s -> "<li>" + toHtmlText(s) + "</li>")
                .reduce("", String::concat);
        return "<ul>" + items + "</ul>";
    }

    private String formatExperience(Object raw) {
        if (!(raw instanceof List<?> items) || items.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> exp)) {
                continue;
            }
            String position = toText(exp.get("position"));
            String company = toText(exp.get("company"));
            String start = toText(exp.get("startDate"));
            String end = toText(exp.get("endDate"));
            boolean current = Boolean.TRUE.equals(exp.get("current"));
            String period = start;
            if (!end.isBlank() || current) {
                period = start + " - " + (current ? "Present" : end);
            }
            String desc = toText(exp.get("description"));
            out.append("<div>")
               .append("<strong>").append(toHtmlText(position)).append("</strong>")
               .append(" - ").append(toHtmlText(company)).append("<br/>")
               .append("<small>").append(toHtmlText(period)).append("</small>")
               .append(desc.isBlank() ? "" : "<p>" + toHtmlText(desc) + "</p>")
               .append("</div>");
        }
        return out.toString();
    }

    private String formatEducation(Object raw) {
        if (!(raw instanceof List<?> items) || items.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> edu)) {
                continue;
            }
            String degree = toText(edu.get("degree"));
            String field = toText(edu.get("field"));
            String institution = toText(edu.get("institution"));
            String start = toText(edu.get("startDate"));
            String end = toText(edu.get("endDate"));
            String desc = toText(edu.get("description"));
            String degreeLine = degree + (field.isBlank() ? "" : " in " + field);
            String period = start + (end.isBlank() ? "" : " - " + end);
            out.append("<div>")
               .append("<strong>").append(toHtmlText(degreeLine)).append("</strong>")
               .append("<br/>")
               .append("<small>").append(toHtmlText(institution)).append("</small>")
               .append(period.isBlank() ? "" : "<div>" + toHtmlText(period) + "</div>")
               .append(desc.isBlank() ? "" : "<p>" + toHtmlText(desc) + "</p>")
               .append("</div>");
        }
        return out.toString();
    }

    private String formatProjects(Object raw) {
        if (!(raw instanceof List<?> items) || items.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> proj)) {
                continue;
            }
            String name = toText(proj.get("name"));
            String tech = toText(proj.get("technologies"));
            String link = toText(proj.get("link"));
            String desc = toText(proj.get("description"));
            out.append("<div>")
               .append("<strong>").append(toHtmlText(name)).append("</strong>")
               .append(link.isBlank() ? "" : " - " + linkOrText(link, "Link"))
               .append(tech.isBlank() ? "" : "<div>Tech: " + toHtmlText(tech) + "</div>")
               .append(desc.isBlank() ? "" : "<p>" + toHtmlText(desc) + "</p>")
               .append("</div>");
        }
        return out.toString();
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




