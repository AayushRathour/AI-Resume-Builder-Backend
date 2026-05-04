package com.resumeai.ai.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.ai.dto.AIHistoryResponse;
import com.resumeai.ai.dto.AIResponse;
import com.resumeai.ai.dto.ATSRequest;
import com.resumeai.ai.dto.ATSResponse;
import com.resumeai.ai.dto.BulletRequest;
import com.resumeai.ai.dto.CoverLetterRequest;
import com.resumeai.ai.dto.ImproveRequest;
import com.resumeai.ai.dto.MissingSkillsRequest;
import com.resumeai.ai.dto.MissingSkillsResponse;
import com.resumeai.ai.dto.QuotaResponse;
import com.resumeai.ai.dto.ResumeExtractRequest;
import com.resumeai.ai.dto.ResumeExtractResponse;
import com.resumeai.ai.dto.SkillRequest;
import com.resumeai.ai.dto.StatusResponse;
import com.resumeai.ai.dto.SummaryRequest;
import com.resumeai.ai.dto.TailorRequest;
import com.resumeai.ai.dto.TranslateRequest;
import com.resumeai.ai.service.AiService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

        @PostMapping({"/summary", "/generate-summary"})
    public ResponseEntity<StatusResponse<AIResponse>> generateSummary(
            @RequestParam Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestBody SummaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(aiService.generateSummary(userId, resumeId, request)));
    }

    @PostMapping("/bullets")
    public ResponseEntity<StatusResponse<AIResponse>> generateBullets(
            @RequestParam Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestBody BulletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(aiService.generateBulletPoints(userId, resumeId, request)));
    }

    @PostMapping("/cover-letter")
    public ResponseEntity<StatusResponse<AIResponse>> generateCoverLetter(
            @RequestParam Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestBody CoverLetterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(aiService.generateCoverLetter(userId, resumeId, request)));
    }

        @PostMapping({"/improve", "/improve-section"})
    public ResponseEntity<StatusResponse<AIResponse>> improveSection(
            @RequestParam Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestBody ImproveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(aiService.improveSection(userId, resumeId, request)));
    }

        @PostMapping({"/ats", "/check-ats"})
    public ResponseEntity<StatusResponse<ATSResponse>> checkAts(
            @RequestParam Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestBody ATSRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(aiService.checkAtsCompatibility(userId, resumeId, request)));
    }

    @PostMapping("/ats-upload")
    public ResponseEntity<StatusResponse<?>> atsUpload(@RequestParam("file") MultipartFile file) {
        // Step 1: Extract text from the uploaded file
        String text;
        try {
            text = extractText(file);
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(fail("Could not extract text from file"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(fail("Unsupported or corrupted file: " + e.getMessage()));
        }

        try {
            ATSRequest request = ATSRequest.builder()
                    .resumeContent(text)
                    .jobDescription("")
                    .build();
            ATSResponse ats = aiService.checkAtsCompatibility(0L, null, request);
            return ResponseEntity.ok(ok(ats));
        } catch (Exception e) {
            String fallbackResult = computeLocalAtsAnalysis(text);
            return ResponseEntity.ok(ok(java.util.Map.of("result", fallbackResult)));
        }
    }

    /**
     * Local keyword-based ATS analysis fallback.
     * Used when Gemini API is unavailable (quota exceeded, network error, etc.).
     */
    private String computeLocalAtsAnalysis(String resumeText) {
        String lower = resumeText.toLowerCase();
        int score = 0;
        java.util.List<String> found = new java.util.ArrayList<>();
        java.util.List<String> missing = new java.util.ArrayList<>();

        // Check for essential resume sections
        String[][] sectionChecks = {
            {"contact information", "email", "phone", "@"},
            {"work experience", "experience", "employment"},
            {"education", "degree", "university", "bachelor", "master", "b.tech", "b.e", "mba"},
            {"skills", "technical skills", "core competencies"},
            {"summary", "objective", "profile", "about me"},
            {"achievements", "accomplishments", "awards"},
            {"certifications", "certified"},
            {"projects", "project"}
        };
        String[] sectionNames = {"Contact Info", "Work Experience", "Education", "Skills", "Summary/Objective", "Achievements", "Certifications", "Projects"};

        for (int i = 0; i < sectionChecks.length; i++) {
            boolean sectionFound = false;
            for (String keyword : sectionChecks[i]) {
                if (lower.contains(keyword)) {
                    sectionFound = true;
                    break;
                }
            }
            if (sectionFound) {
                found.add(sectionNames[i]);
                score += (i < 5) ? 12 : 8; // Core sections worth more
            } else {
                missing.add(sectionNames[i]);
            }
        }

        // Check for action verbs (strong ATS signal)
        String[] actionVerbs = {"led", "managed", "developed", "designed", "implemented", "created", "improved", "increased", "reduced", "achieved", "built", "launched", "delivered", "optimized"};
        int verbCount = 0;
        for (String verb : actionVerbs) {
            if (lower.contains(verb)) verbCount++;
        }
        score += Math.min(16, verbCount * 4);

        // Check for quantifiable results
        boolean hasNumbers = resumeText.matches("(?s).*\\d+%.*") || resumeText.matches("(?s).*\\$\\d+.*") || resumeText.matches("(?s).*\\d+\\+.*");
        if (hasNumbers) {
            score += 8;
            found.add("Quantifiable metrics");
        } else {
            missing.add("Quantifiable metrics (add numbers like percentages, dollar amounts)");
        }

        score = Math.max(15, Math.min(100, score));

        // Build human-readable result
        StringBuilder sb = new StringBuilder();
        sb.append("ATS Score: ").append(score).append("/100\n\n");
        sb.append("✅ Found Sections: ").append(String.join(", ", found)).append("\n\n");

        if (!missing.isEmpty()) {
            sb.append("❌ Missing/Weak Areas: ").append(String.join(", ", missing)).append("\n\n");
        }

        sb.append("Suggestions:\n");
        sb.append("1. ").append(missing.isEmpty() ? "Your resume covers all key sections — great job!" : "Add these missing sections: " + String.join(", ", missing)).append("\n");
        if (verbCount < 5) {
            sb.append("2. Use more action verbs (led, managed, developed, implemented, etc.)\n");
        }
        if (!hasNumbers) {
            sb.append("3. Add quantifiable achievements (e.g., 'Increased sales by 20%')\n");
        }
        sb.append("4. Keep formatting simple — avoid tables, images, and fancy layouts for ATS\n");
        sb.append("5. Use standard section headings like 'Experience', 'Education', 'Skills'\n");

        sb.append("\n⚠️ Note: This analysis was performed using keyword matching. AI-powered analysis is temporarily unavailable.");

        return sb.toString();
    }

    private String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) return "";
        filename = filename.toLowerCase();

        try (InputStream is = file.getInputStream()) {
            if (filename.endsWith(".pdf")) {
                PDDocument document = PDDocument.load(is);
                PDFTextStripper pdfStripper = new PDFTextStripper();
                String text = pdfStripper.getText(document);
                document.close();
                System.out.println("EXTRACTED TEXT LENGTH: " + (text == null ? 0 : text.length()));
                if (text == null || text.trim().isEmpty()) {
                    throw new RuntimeException("PDF TEXT EMPTY");
                }
                return text;
            } else if (filename.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(is);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            } else if (filename.endsWith(".txt")) {
                return new String(is.readAllBytes());
            }
        }
        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }

    @PostMapping("/skills")
    public ResponseEntity<StatusResponse<AIResponse>> suggestSkills(
            @RequestParam Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestBody SkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(aiService.suggestSkills(userId, resumeId, request)));
    }

    @PostMapping("/tailor")
    public ResponseEntity<StatusResponse<AIResponse>> tailorResume(
            @RequestParam Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestBody TailorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(aiService.tailorResumeForJob(userId, resumeId, request)));
    }

    @PostMapping("/translate")
    public ResponseEntity<StatusResponse<AIResponse>> translateResume(
            @RequestParam Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestBody TranslateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(aiService.translateResume(userId, resumeId, request)));
    }

    @PostMapping("/resume-extract")
    public ResponseEntity<Map<String, Object>> extractResume(@RequestBody ResumeExtractRequest request) {
        System.out.println("API HIT: /resume-extract");
        if (request == null) {
            throw new RuntimeException("Request is NULL");
        }
        if (request.getResumeText() == null || request.getResumeText().trim().isEmpty()) {
            throw new RuntimeException("PDF TEXT EMPTY");
        }
        System.out.println("EXTRACTED TEXT LENGTH: " + request.getResumeText().length());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", aiService.extractResumeData(request)
        ));
    }

    @PostMapping(value = "/resume-extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> extractResumeFromFile(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestPart(required = false, value = "file") MultipartFile file) {
        System.out.println("API HIT: /resume-extract");

        if (file == null) {
            throw new RuntimeException("File is NULL");
        }

        String text;
        try {
            text = extractText(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from uploaded file: " + e.getMessage(), e);
        }

        System.out.println("EXTRACTED TEXT LENGTH: " + (text == null ? 0 : text.length()));
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("PDF TEXT EMPTY");
        }

        ResumeExtractRequest request = ResumeExtractRequest.builder()
                .userId(userId)
                .resumeId(resumeId)
                .resumeText(text)
                .build();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", aiService.extractResumeData(request)
        ));
    }

    @PostMapping("/missing-skills")
    public ResponseEntity<StatusResponse<MissingSkillsResponse>> analyzeMissingSkills(@RequestBody MissingSkillsRequest request) {
        return ResponseEntity.ok(ok(aiService.analyzeMissingSkills(request)));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<StatusResponse<List<AIHistoryResponse>>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(ok(aiService.getAiHistory(userId)));
    }

    @GetMapping("/quota/{userId}")
    public ResponseEntity<StatusResponse<QuotaResponse>> getQuota(@PathVariable Long userId) {
        return ResponseEntity.ok(ok(aiService.getRemainingQuota(userId)));
    }

    private <T> StatusResponse<T> ok(T data) {
        return StatusResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    private StatusResponse<?> fail(String message) {
        return StatusResponse.builder()
                .status("failed")
                .message(message)
                .build();
    }
}
