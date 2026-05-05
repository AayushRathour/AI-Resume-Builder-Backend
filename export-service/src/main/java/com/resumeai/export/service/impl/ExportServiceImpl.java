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
            boolean hasEmbeddedFields = hasEmbeddedFields(resume);
            if (!hasSections && !hasDynamicSections && !hasEmbeddedFields) {
                log.info("Export requested with no sections or embedded fields; proceeding with empty PDF.");
            }

                Long effectiveTemplateId = templateId != null ? templateId : resume.getTemplateId();
                TemplateDTO template = effectiveTemplateId != null
                    ? templateClient.getTemplateById(effectiveTemplateId)
                    : null;
                debugTemplateHtml(effectiveTemplateId, template);

            String payload = "JSON".equals(format)
                    ? buildJsonPayload(resume, sections, template)
                    : buildHtmlPayload(resume, sections, template);

            if (!"JSON".equals(format)) {
                log.info("PDF HTML length: {}", payload.length());
                log.debug("PDF HTML: {}", payload);
            }
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
        if (resume.getSectionsJson() != null && !resume.getSectionsJson().isBlank()) {
            try {
                String baseTemplate = template != null && template.getHtmlContent() != null && !template.getHtmlContent().isBlank()
                    ? template.getHtmlContent()
                    : DEFAULT_EMPTY_TEMPLATE;
                String normalizedTemplate = normalizeTemplateHtml(baseTemplate);
                String html = renderTemplateFromSectionsJson(normalizedTemplate, resume.getSectionsJson());
                log.info("PDF HTML compiled for resumeId={} (chars={})", resume.getResumeId(), html.length());
                String css = template != null ? template.getCssContent() : null;
                return finalizeHtml(html, css);
            } catch (Exception ex) {
                log.warn("Failed to render template with sectionsJson, falling back to legacy: {}", ex.getMessage());
            }
        }

        if (hasEmbeddedFields(resume)) {
            try {
                String baseTemplate = template != null && template.getHtmlContent() != null && !template.getHtmlContent().isBlank()
                    ? template.getHtmlContent()
                    : DEFAULT_EMPTY_TEMPLATE;
                String normalizedTemplate = normalizeTemplateHtml(baseTemplate);
                String synthesizedJson = buildSectionsJsonFromResume(resume);
                String html = renderTemplateFromSectionsJson(normalizedTemplate, synthesizedJson);
                String css = template != null ? template.getCssContent() : null;
                return finalizeHtml(html, css);
            } catch (Exception ex) {
                log.warn("Failed to render template from embedded resume fields: {}", ex.getMessage());
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
        return finalizeHtml(body.toString(), css);
    }

    private String defaultCss() {
        return "body{font-family:Arial,sans-serif;margin:40px;color:#333;}"
                + ".resume-header{border-bottom:2px solid #333;padding-bottom:10px;margin-bottom:20px;}"
                + ".resume-header h1{margin:0;font-size:28px;}"
                + ".section{margin-bottom:20px;}"
                + ".section h2{font-size:18px;border-bottom:1px solid #ccc;padding-bottom:4px;}";
    }

    private boolean hasEmbeddedFields(ResumeDTO resume) {
        return resume != null && (
                isNotBlank(resume.getName())
                || isNotBlank(resume.getTitle())
                || isNotBlank(resume.getEmail())
                || isNotBlank(resume.getPhone())
                || isNotBlank(resume.getLocation())
                || isNotBlank(resume.getSummary())
                || isNotBlank(resume.getSkills())
                || isNotBlank(resume.getExperience())
                || isNotBlank(resume.getEducation())
                || isNotBlank(resume.getProjects())
        );
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String buildSectionsJsonFromResume(ResumeDTO resume) throws JsonProcessingException {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> personal = new LinkedHashMap<>();
        personal.put("name", toText(resume.getName()));
        personal.put("title", toText(resume.getTitle()));
        personal.put("email", toText(resume.getEmail()));
        personal.put("phone", toText(resume.getPhone()));
        personal.put("location", toText(resume.getLocation()));
        data.put("personal", personal);
        data.put("summary", toText(resume.getSummary()));
        data.put("skills", parseJsonList(resume.getSkills()));
        data.put("experience", parseJsonList(resume.getExperience()));
        data.put("education", parseJsonList(resume.getEducation()));
        data.put("projects", parseJsonList(resume.getProjects()));
        return OBJECT_MAPPER.writeValueAsString(data);
    }

    private List<Object> parseJsonList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            Object parsed = OBJECT_MAPPER.readValue(raw, Object.class);
            if (parsed instanceof List<?> list) {
                return List.copyOf(list);
            }
        } catch (Exception ex) {
            log.debug("Failed to parse JSON list: {}", ex.getMessage());
        }
        return List.of();
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
        html = replaceToken(html, "linkedin", linkOrText(linkedin, "LinkedIn"));
        html = replaceToken(html, "github", linkOrText(github, "GitHub"));
        html = replaceToken(html, "website", linkOrText(website, "Portfolio"));

        String rawSummary = toText(data.get("summary"));
        String safeSummary = sanitizeSummaryText(rawSummary);
        html = replaceToken(html, "summary", toHtmlText(safeSummary));
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
        String safeBody = html == null ? "" : stripHtmlWrapper(html);
        ExtractedStyle extracted = extractInlineStyles(safeBody);
        safeBody = extracted.bodyHtml();
        String mergedCss = mergeCss(css, extracted.css());
        String layoutOverrides = buildPdfLayoutOverrides(safeBody);
        mergedCss = mergeCss(mergedCss, layoutOverrides);
        String safeCss = mergedCss == null ? "" : sanitizeCssForXml(mergedCss);
        String doc = buildXhtmlDocument(safeBody, safeCss);
        return sanitizeXmlHtml(doc);
    }

    private String buildXhtmlDocument(String bodyHtml, String css) {
        StringBuilder head = new StringBuilder();
        head.append("<meta charset=\"UTF-8\" />");
        if (css != null && !css.isBlank()) {
            head.append("<style>").append(css).append("</style>");
        }
        return "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
                + head + "</head><body>" + bodyHtml + "</body></html>";
    }

    private String stripHtmlWrapper(String html) {
        String cleaned = html;
        cleaned = cleaned.replaceAll("(?is)<!DOCTYPE[^>]*>", "");
        cleaned = cleaned.replaceAll("(?is)<\\/?html[^>]*>", "");
        cleaned = cleaned.replaceAll("(?is)<\\/?head[^>]*>.*?<\\/head>", "");
        cleaned = cleaned.replaceAll("(?is)<\\/?body[^>]*>", "");
        return cleaned.trim();
    }

    private String normalizeTemplateHtml(String templateHtml) {
        if (templateHtml == null || templateHtml.isBlank()) {
            return "";
        }
        String cleaned = stripHtmlWrapper(templateHtml);
        // Remove meta tags from templates (we inject a clean one later)
        cleaned = cleaned.replaceAll("(?i)</meta>", "");
        cleaned = cleaned.replaceAll("(?i)<meta\\s*[^>]*>", "");
        // Ensure void tags are self-closed before XHTML wrapping
        cleaned = cleaned.replaceAll("(?i)<br([^>/]*?)>", "<br$1 />");
        cleaned = cleaned.replaceAll("(?i)<hr([^>/]*?)>", "<hr$1 />");
        cleaned = cleaned.replaceAll("(?i)<img([^>/]*?)>", "<img$1 />");
        cleaned = cleaned.replaceAll("(?i)<input([^>/]*?)>", "<input$1 />");
        cleaned = cleaned.replaceAll("(?i)<link([^>/]*?)>", "<link$1 />");
        return cleaned;
    }

    private String sanitizeXmlHtml(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        boolean hadFamilyParam = html.contains("&family=");
        String sanitized = forceCloseMetaTags(html);
        // Ensure void tags are XML-compliant
        sanitized = sanitized.replaceAll("(?i)<meta(?![^>]*?/>)\\s*([^>]*)>", "<meta$1 />");
        sanitized = sanitized.replaceAll("(?i)<link(?![^>]*?/>)\\s*([^>]*)>", "<link$1 />");
        sanitized = sanitized.replaceAll("(?i)<br(?![^>]*?/>)\\s*([^>]*)>", "<br$1 />");
        sanitized = sanitized.replaceAll("(?i)<hr(?![^>]*?/>)\\s*([^>]*)>", "<hr$1 />");
        sanitized = sanitized.replaceAll("(?i)<img(?![^>]*?/>)\\s*([^>]*)>", "<img$1 />");
        sanitized = sanitized.replaceAll("(?i)<input(?![^>]*?/>)\\s*([^>]*)>", "<input$1 />");
        // Escape stray ampersands (e.g., in href query params) for XML
        sanitized = sanitized.replaceAll("&(?!amp;|lt;|gt;|quot;|apos;|#\\d+;|#x[0-9a-fA-F]+;)", "&amp;");
        if (hadFamilyParam) {
            log.info("Sanitized ampersands in template HTML for PDF rendering");
        }
        return sanitized;
    }

    private String sanitizeCssForXml(String css) {
        if (css == null || css.isBlank()) {
            return css;
        }
        return css.replaceAll("&(?!amp;|lt;|gt;|quot;|apos;|#\\d+;|#x[0-9a-fA-F]+;)", "&amp;");
    }

    private String mergeCss(String primary, String secondary) {
        if (primary == null || primary.isBlank()) {
            return secondary;
        }
        if (secondary == null || secondary.isBlank()) {
            return primary;
        }
        return primary + "\n" + secondary;
    }

    private ExtractedStyle extractInlineStyles(String html) {
        if (html == null || html.isBlank()) {
            return new ExtractedStyle("", "");
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?is)<style[^>]*>(.*?)</style>");
        java.util.regex.Matcher matcher = pattern.matcher(html);
        StringBuilder css = new StringBuilder();
        String cleaned = html;
        while (matcher.find()) {
            css.append(matcher.group(1)).append("\n");
        }
        cleaned = matcher.replaceAll("");
        return new ExtractedStyle(cleaned, css.toString().trim());
    }

    private record ExtractedStyle(String bodyHtml, String css) {}

    private String removeMetaTags(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        String cleaned = html.replaceAll("(?i)</meta>", "");
        return cleaned.replaceAll("(?is)<meta\\b[^>]*>", "");
    }

    private String forceCloseMetaTags(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        String cleaned = html.replaceAll("(?i)</meta>", "");
        return cleaned.replaceAll("(?i)<meta(?![^>]*?/>)\\s*([^>]*)>", "<meta$1 />");
    }

    private void debugTemplateHtml(Long templateId, TemplateDTO template) {
        if (templateId == null || template == null) {
            return;
        }
        String html = template.getHtmlContent();
        if (html == null || html.isBlank()) {
            return;
        }
        try {
            Path dir = Paths.get(outputDir);
            Files.createDirectories(dir);
            Path debugPath = dir.resolve("debug_template_" + templateId + ".html");
            Files.writeString(debugPath, html);
            boolean hasMeta = html.toLowerCase(Locale.ROOT).contains("<meta");
            log.info("Template {} HTML captured (meta tags present: {})", templateId, hasMeta);
        } catch (Exception ex) {
            log.warn("Failed to write template debug HTML for templateId={}: {}", templateId, ex.getMessage());
        }
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

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toHtmlText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String escaped = escapeHtml(value);
        return escaped.replace("\n", "<br/>");
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    private String sanitizeSummaryText(String summary) {
        if (summary == null) {
            return "";
        }
        String upper = summary.toUpperCase(Locale.ROOT);
        if (upper.contains("DROP") || upper.contains("CREATE") || upper.contains("USE")) {
            return "Invalid content removed";
        }
        if (upper.contains("<!DOCTYPE")
                || upper.contains("<HTML")
                || upper.contains("<HEAD")
                || upper.contains("<META")
                || upper.contains("<LINK")) {
            return "Invalid content removed";
        }
        return summary;
    }

    private String buildPdfLayoutOverrides(String bodyHtml) {
        if (bodyHtml == null || bodyHtml.isBlank()) {
            return "";
        }
        boolean hasPage = bodyHtml.contains("class=\"page\"");
        boolean hasSidebar = bodyHtml.contains("class=\"sidebar\"");
        boolean hasMain = bodyHtml.contains("class=\"main\"");
        if (!hasPage || !hasSidebar || !hasMain) {
            return "";
        }
        return ".page{display:table;width:794px;table-layout:fixed;}"
            + ".sidebar{display:table-cell;width:248px;vertical-align:top;}"
            + ".main{display:table-cell;vertical-align:top;}";
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
                String xhtml = sanitizeXmlHtml(content);
                Path debugPath = dir.resolve("debug_last.xhtml");
                Files.writeString(debugPath, xhtml);
                log.debug("FINAL HTML: {}", xhtml);
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(xhtml, null);
                builder.toStream(os);
                builder.run();
            } catch (Exception ex) {
                log.warn("PDF render failed; saving HTML fallback: {}", ex.getMessage());
                String fallbackName = fileName.replace(".pdf", ".html");
                Path fallbackPath = dir.resolve(fallbackName);
                Files.writeString(fallbackPath, content);
                return fallbackPath.toAbsolutePath().toString();
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
