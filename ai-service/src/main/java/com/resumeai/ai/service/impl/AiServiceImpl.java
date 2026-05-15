package com.resumeai.ai.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.dto.AIHistoryResponse;
import com.resumeai.ai.dto.AIResponse;
import com.resumeai.ai.dto.ATSRequest;
import com.resumeai.ai.dto.ATSResponse;
import com.resumeai.ai.dto.BulletRequest;
import com.resumeai.ai.dto.CoverLetterRequest;
import com.resumeai.ai.dto.ImproveRequest;
import com.resumeai.ai.dto.MissingSkillsRequest;
import com.resumeai.ai.dto.MissingSkillsResponse;
import com.resumeai.ai.dto.NotificationEvent;
import com.resumeai.ai.dto.ChatRequest;
import com.resumeai.ai.dto.QuotaResponse;
import com.resumeai.ai.dto.ResumeExtractRequest;
import com.resumeai.ai.dto.ResumeExtractResponse;
import com.resumeai.ai.dto.SkillRequest;
import com.resumeai.ai.dto.SummaryRequest;
import com.resumeai.ai.dto.TailorRequest;
import com.resumeai.ai.dto.TranslateRequest;
import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.entity.RequestStatus;
import com.resumeai.ai.entity.RequestType;
import com.resumeai.ai.exception.AiServiceException;
import com.resumeai.ai.exception.QuotaExceededException;
import com.resumeai.ai.repository.AiRequestRepository;
import com.resumeai.ai.service.AiService;
import com.resumeai.ai.provider.AiProviderFactory;
import com.resumeai.ai.provider.AiProviderFactory.AiProviderResult;
import com.resumeai.ai.util.PromptBuilder;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrates AI generation workflows, quota checks, request persistence,
 * and completion notifications for ai-service operations.
 */

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final AiRequestRepository repository;
    private final ObjectMapper objectMapper;
    private final AiProviderFactory providerFactory;
    private final RabbitTemplate rabbitTemplate;

    @Value("${ai.quota.free.monthly-calls:5}")
    private int freeMonthlyCallsLimit;

    @Value("${ai.quota.free.monthly-ats:5}")
    private int freeMonthlyAtsLimit;

    @Value("${rabbitmq.exchange:notification.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.ai:ai.completed}")
    private String aiRoutingKey;

    @Value("${ai.request-timeout-seconds:5}")
    private int aiRequestTimeoutSeconds;


    @Override
    public AIResponse generateSummary(Long userId, Long resumeId, SummaryRequest req) {
        enforceQuota(userId, RequestType.SUMMARY);
        String prompt = PromptBuilder.buildSummaryPrompt(
            req.getJobTitle(), req.getYearsOfExperience(), req.getKeySkills(), req.getAdditionalContext()
        );
        return executeAndSave(userId, resumeId, RequestType.SUMMARY, prompt);
    }

    @Override
    public AIResponse generateBulletPoints(Long userId, Long resumeId, BulletRequest req) {
        enforceQuota(userId, RequestType.BULLETS);
        String prompt = PromptBuilder.buildBulletsPrompt(
            req.getJobTitle(), req.getCompanyName(), req.getResponsibilities(), req.getAchievements()
        );
        return executeAndSave(userId, resumeId, RequestType.BULLETS, prompt);
    }

    @Override
    public AIResponse generateCoverLetter(Long userId, Long resumeId, CoverLetterRequest req) {
        enforceQuota(userId, RequestType.COVER_LETTER);
        String prompt = PromptBuilder.buildCoverLetterPrompt(
            req.getJobTitle(), req.getCompanyName(), req.getJobDescription(), req.getApplicantSummary()
        );
        return executeAndSave(userId, resumeId, RequestType.COVER_LETTER, prompt);
    }

    @Override
    public AIResponse improveSection(Long userId, Long resumeId, ImproveRequest req) {
        enforceQuota(userId, RequestType.IMPROVE);
        String prompt = PromptBuilder.buildImprovePrompt(
            req.getSectionType(), req.getCurrentContent(), req.getTargetRole()
        );
        return executeAndSave(userId, resumeId, RequestType.IMPROVE, prompt);
    }

    @Override
    public ATSResponse checkAtsCompatibility(Long userId, Long resumeId, ATSRequest req) {
        String resumeContent = req.getResumeContent() == null ? "" : req.getResumeContent();
        String jobDescription = req.getJobDescription() == null ? "" : req.getJobDescription();

        if (resumeContent.isBlank()) {
            return ATSResponse.builder()
                    .score(0)
                    .missingKeywords(List.of("resume content"))
                    .recommendations("Could not extract readable resume content. Upload a valid PDF/DOCX/TXT.")
                    .build();
        }

        // ATS upload flow sends userId=0; skip strict quota there so standalone checks keep working.
        if (userId != null && userId > 0) {
            enforceAtsQuota(userId);
        }

        // ATS upload flow often has no JD; return deterministic score immediately to keep checks fast.
        if (jobDescription.isBlank()) {
            ATSResponse standalone = buildAtsFallbackResponse(resumeContent, "", null);
            standalone.setRecommendations(standalone.getRecommendations()
                    + "\n\nFast mode used: computed without external AI for quick response.");
            return standalone;
        }

        // Route ATS checks through provider fallback so scoring remains available during provider outages.
        String prompt = PromptBuilder.buildAtsPrompt(resumeContent, jobDescription);

        AiRequest record = saveQueued(userId, resumeId, RequestType.ATS, prompt);

        try {
            AiProviderResult aiResult = executeWithTimeout(() -> providerFactory.generateWithFallback(prompt), "ATS_CHECK");
            record.setModel(aiResult.model());
            ATSResponse atsResponse = parseAtsResponse(aiResult.text(), resumeContent, jobDescription);
            markCompleted(record, aiResult.text(), estimateTokens(aiResult.text()));
            atsResponse.setRequestId(record.getRequestId());
            return atsResponse;
        } catch (QuotaExceededException ex) {
            log.warn("AI quota exceeded. Falling back to hybrid ATS scoring.");
            ATSResponse atsResponse = buildAtsFallbackResponse(resumeContent, jobDescription, record.getRequestId());
            markCompleted(record, "AI TEMPORARILY UNAVAILABLE", 0);
            return atsResponse;
        } catch (Exception ex) {
            markFailed(record);
            return buildAtsFallbackResponse(resumeContent, jobDescription, record.getRequestId());
        }
    }

    @Override
    public AIResponse suggestSkills(Long userId, Long resumeId, SkillRequest req) {
        enforceQuota(userId, RequestType.SKILLS);
        String prompt = PromptBuilder.buildSkillsPrompt(
            req.getJobTitle(), req.getCurrentSkills(), req.getIndustry()
        );
        return executeAndSave(userId, resumeId, RequestType.SKILLS, prompt);
    }

    @Override
    public AIResponse tailorResumeForJob(Long userId, Long resumeId, TailorRequest req) {
        enforceQuota(userId, RequestType.TAILOR);
        String prompt = PromptBuilder.buildTailorPrompt(
            req.getResumeContent(), req.getJobDescription(), req.getJobTitle()
        );
        return executeAndSave(userId, resumeId, RequestType.TAILOR, prompt);
    }

    @Override
    public AIResponse translateResume(Long userId, Long resumeId, TranslateRequest req) {
        enforceQuota(userId, RequestType.TRANSLATE);
        String prompt = PromptBuilder.buildTranslatePrompt(
            req.getResumeContent(), req.getTargetLanguage()
        );
        return executeAndSave(userId, resumeId, RequestType.TRANSLATE, prompt);
    }

    @Override
    public AIResponse chat(Long userId, ChatRequest req) {
        enforceQuota(userId, RequestType.CHAT);
        String prompt = PromptBuilder.buildChatPrompt(req.getMessage(), req.getContext());
        // For general chat, we might not have a resumeId, so we pass null or 0
        return executeAndSave(userId, null, RequestType.CHAT, prompt);
    }

    @Override
    public ResumeExtractResponse extractResumeData(ResumeExtractRequest request) {
        if (request == null) {
            throw new RuntimeException("Request is NULL");
        }

        String resumeText = request.getResumeText() == null ? "" : request.getResumeText();
        if (resumeText.trim().isEmpty()) {
            throw new RuntimeException("PDF TEXT EMPTY");
        }

        log.info("[AI] EXTRACTED TEXT LENGTH: {}", resumeText.length());

        String prompt = """
You are an AI resume parser.

Extract structured data from the resume.

Return ONLY valid JSON in this format:

{
"skills": [],
"roles": [],
"experience": "",
"keywords": []
}

Rules:

* Extract ALL technical skills (ML, AI, backend, tools)
* Extract ALL possible job roles
* Estimate experience from projects + work
* Do NOT return explanation
* Do NOT add text outside JSON

Resume:
""" + resumeText;

        AiRequest record;
        boolean persisted = true;
        try {
            record = saveQueued(request.getUserId(), request.getResumeId(), RequestType.RESUME_EXTRACT, prompt);
        } catch (Exception saveException) {
            persisted = false;
            log.warn("Could not persist resume-extract request: {}", saveException.getMessage());
            record = AiRequest.builder()
                    .requestId(UUID.randomUUID().toString())
                    .userId(request.getUserId())
                    .resumeId(request.getResumeId())
                    .requestType(RequestType.RESUME_EXTRACT)
                    .inputPrompt(prompt)
                    .status(RequestStatus.QUEUED)
                    .model("pending")
                    .build();
        }

        try {
            AiProviderResult aiResult = providerFactory.generateWithFallback(prompt);
            record.setModel(aiResult.model());
            String rawText = aiResult.text();
            String cleaned = cleanJsonString(rawText);
            ResumeExtractResponse response = objectMapper.readValue(cleaned, ResumeExtractResponse.class);
            if (persisted) {
                markCompleted(record, rawText, estimateTokens(rawText));
            }
            return normalizeResumeExtract(response);
        } catch (Exception ex) {
            if (persisted) {
                markFailed(record);
            }
            return ResumeExtractResponse.builder()
                    .skills(List.of())
                    .roles(List.of())
                    .keywords(List.of())
                    .experience("AI TEMPORARILY UNAVAILABLE")
                    .build();
        }
    }

    @Override
    public MissingSkillsResponse analyzeMissingSkills(MissingSkillsRequest request) {
        String resumeText = request.getResumeText() == null ? "" : request.getResumeText();
        String jobDescription = request.getJobDescription() == null ? "" : request.getJobDescription();
        String prompt = """
You are a resume analyzer comparing candidate skills with job requirements.

Compare the candidate's resume with the job description.

Identify missing skills and suggest recommendations to improve the match.

Return ONLY a JSON object with this exact structure:

{
"missingSkills": "Comma separated list of missing skills",
"recommendations": "Brief actionable advice"
}

Rules:
* Extract ONLY missing technical skills
* Be specific and practical
* Do NOT return explanation
* Do NOT add text outside JSON

Resume:
""" + resumeText + """

Job Description:
""" + jobDescription;

        AiRequest record = saveQueued(request.getUserId(), request.getResumeId(), RequestType.MISSING_SKILLS, prompt);
        try {
            AiProviderResult aiResult = providerFactory.generateWithFallback(prompt);
            record.setModel(aiResult.model());
            String rawText = aiResult.text();
            String cleaned = cleanJsonString(rawText);
            MissingSkillsResponse response = objectMapper.readValue(cleaned, MissingSkillsResponse.class);
            markCompleted(record, rawText, estimateTokens(rawText));
            return response;
        } catch (Exception ex) {
            markFailed(record);
            return MissingSkillsResponse.builder()
                    .missingSkills("")
                    .recommendations("AI TEMPORARILY UNAVAILABLE")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIHistoryResponse> getAiHistory(Long userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaResponse getRemainingQuota(Long userId) {
        long callsUsed = repository.countByUserIdCurrentMonth(userId);
        long atsUsed = repository.countByUserIdAndRequestTypeCurrentMonth(userId, RequestType.ATS);

        long remaining = Math.max(0, freeMonthlyCallsLimit - callsUsed);
        long atsRemaining = Math.max(0, freeMonthlyAtsLimit - atsUsed);

        return QuotaResponse.builder()
                .userId(userId)
                .isPremium(false) // premium logic placeholder
                .callsUsedThisMonth(callsUsed)
                .callsAllowed(freeMonthlyCallsLimit)
                .atsChecksUsedThisMonth(atsUsed)
                .atsChecksAllowed(freeMonthlyAtsLimit)
                .remainingCalls(remaining)
                .remainingAtsChecks(atsRemaining)
                .build();
    }


    private AIResponse executeAndSave(Long userId, Long resumeId, RequestType type, String prompt) {
        // Standard request lifecycle: queue, execute via provider, then persist completion state.
        AiRequest record = saveQueued(userId, resumeId, type, prompt);
        try {
            AiProviderResult aiResult = executeWithTimeout(
                    () -> providerFactory.generateWithFallback(prompt),
                    type.name()
            );
            record.setModel(aiResult.model());
            int tokens = estimateTokens(aiResult.text());
            markCompleted(record, aiResult.text(), tokens);
            return AIResponse.builder()
                    .text(aiResult.text())
                    .model(aiResult.model())
                    .tokensUsed(tokens)
                    .requestId(record.getRequestId())
                    .build();
        } catch (Exception ex) {
            markFailed(record);
            String fallbackModel = "unavailable";
            try { fallbackModel = providerFactory.getProvider().getModelName(); } catch (Exception ignored) {}
            return AIResponse.builder()
                    .text("AI TEMPORARILY UNAVAILABLE")
                    .model(fallbackModel)
                    .tokensUsed(0)
                    .requestId(record.getRequestId())
                    .build();
        }
    }

    // which supports Gemini, NVIDIA, and future providers with automatic fallback.

    private AiRequest saveQueued(Long userId, Long resumeId, RequestType type, String prompt) {
        AiRequest req = AiRequest.builder()
                .userId(userId)
                .resumeId(resumeId)
                .requestType(type)
                .inputPrompt(prompt)
                .status(RequestStatus.QUEUED)
                .build();
        return repository.save(req);
    }

    private void markCompleted(AiRequest req, String response, int tokens) {
        req.setAiResponse(response);
        req.setTokensUsed(tokens);
        req.setStatus(RequestStatus.COMPLETED);
        req.setCompletedAt(LocalDateTime.now());
        repository.save(req);

        // Publish ai.completed so notification-service can deliver realtime completion updates.
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(req.getUserId())
                    .subject("AI Enhancement Complete  ResumeAI")
                    .message("Your AI " + req.getRequestType().name().toLowerCase()
                            + " request has been completed successfully.")
                    .build();
            rabbitTemplate.convertAndSend(exchange, aiRoutingKey, event);
            log.debug("Published ai.completed event for userId={}", req.getUserId());
        } catch (Exception ex) {
            log.warn("Failed to publish ai.completed event for userId={}: {}", req.getUserId(), ex.getMessage());
        }
    }

    private void markFailed(AiRequest req) {
        req.setStatus(RequestStatus.FAILED);
        req.setCompletedAt(LocalDateTime.now());
        repository.save(req);
    }

    private void enforceQuota(Long userId, RequestType type) {
        long used = repository.countByUserIdCurrentMonth(userId);
        if (used >= freeMonthlyCallsLimit) {
            throw new QuotaExceededException(
                "Monthly AI call quota exceeded. Used: " + used + "/" + freeMonthlyCallsLimit +
                ". Upgrade to Premium for unlimited access."
            );
        }
    }

    private void enforceAtsQuota(Long userId) {
        long atsUsed = repository.countByUserIdAndRequestTypeCurrentMonth(userId, RequestType.ATS);
        if (atsUsed >= freeMonthlyAtsLimit) {
            throw new QuotaExceededException(
                "Monthly ATS check quota exceeded. Used: " + atsUsed + "/" + freeMonthlyAtsLimit +
                ". Upgrade to Premium for unlimited ATS checks."
            );
        }
        enforceQuota(userId, RequestType.ATS);
    }

    private ATSResponse parseAtsResponse(String rawText, String resumeContent, String jobDescription) {
        // Blend AI output with deterministic keyword analysis for stable and explainable ATS scoring.
        String cleaned = cleanJsonString(rawText);
        boolean hasJobDescription = jobDescription != null && !jobDescription.isBlank();
        AtsHybridResult hybridResult = computeHybridAts(resumeContent, jobDescription);
        AtsStandaloneResult standalone = computeStandaloneAts(resumeContent);
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            int aiScore = node.path("score").asInt(0);
            int finalScore;
            List<String> keywords;

            if (hasJobDescription) {
                finalScore = Math.max(0, Math.min(100,
                        Math.round((hybridResult.keywordScore * 0.7f) + (aiScore * 0.3f))));
                keywords = new ArrayList<>(hybridResult.missingKeywords);
            } else {
                // ATS upload mode often has no JD; avoid near-zero scores caused by empty JD keyword match.
                finalScore = Math.max(standalone.score(), aiScore);
                keywords = new ArrayList<>(standalone.missingItems());
            }

            List<String> aiMissingKeywords = Arrays.asList(
                objectMapper.convertValue(node.path("missingKeywords"), String[].class)
            );
            for (String keyword : aiMissingKeywords) {
                if (keyword != null && !keyword.isBlank() && !keywords.contains(keyword)) {
                    keywords.add(keyword);
                }
            }
            String recommendations = node.path("recommendations").asText("");
            return ATSResponse.builder()
                    .score(finalScore)
                    .missingKeywords(keywords)
                    .recommendations(hasJobDescription
                            ? mergeRecommendations(recommendations, hybridResult)
                            : mergeStandaloneRecommendations(recommendations, standalone))
                    .build();
        } catch (JsonProcessingException ex) {
            log.warn("Could not parse ATS JSON response, returning raw text as recommendations");
            if (!hasJobDescription) {
                return ATSResponse.builder()
                        .score(standalone.score())
                        .missingKeywords(standalone.missingItems())
                        .recommendations(mergeStandaloneRecommendations(rawText, standalone))
                        .build();
            }
            return ATSResponse.builder()
                    .score(hybridResult.keywordScore)
                    .missingKeywords(hybridResult.missingKeywords)
                    .recommendations(mergeRecommendations(rawText, hybridResult))
                    .build();
        }
    }

    private String mergeRecommendations(String aiRecommendations, AtsHybridResult hybrid) {
        StringBuilder builder = new StringBuilder();
        if (aiRecommendations != null && !aiRecommendations.isBlank()) {
            builder.append(aiRecommendations.trim());
        }
        if (!hybrid.missingKeywords.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("Keyword alignment suggestions: add these missing keywords where relevant: ")
                    .append(String.join(", ", hybrid.missingKeywords));
        }
        return builder.toString();
    }

    private String mergeStandaloneRecommendations(String aiRecommendations, AtsStandaloneResult standalone) {
        StringBuilder builder = new StringBuilder();
        if (aiRecommendations != null && !aiRecommendations.isBlank()) {
            builder.append(aiRecommendations.trim());
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(standalone.recommendations());
        return builder.toString();
    }

    private ATSResponse buildAtsFallbackResponse(String resumeContent, String jobDescription, String requestId) {
        AtsStandaloneResult standalone = computeStandaloneAts(resumeContent);
        if (jobDescription == null || jobDescription.isBlank()) {
            return ATSResponse.builder()
                    .score(standalone.score())
                    .missingKeywords(standalone.missingItems())
                    .recommendations(standalone.recommendations() + "\n\nAI temporarily unavailable. Showing static ATS result.")
                    .requestId(requestId)
                    .build();
        }

        AtsHybridResult hybridResult = computeHybridAts(resumeContent, jobDescription);
        StringBuilder recommendations = new StringBuilder("AI temporarily unavailable. Showing keyword-based ATS result.");
        recommendations.append("\n\nFormat/Readability: ").append(standalone.recommendations());
        if (!hybridResult.missingKeywords.isEmpty()) {
            recommendations.append("\nAdd these missing keywords where relevant: ")
                    .append(String.join(", ", hybridResult.missingKeywords));
        }
        return ATSResponse.builder()
                .score(hybridResult.keywordScore)
                .missingKeywords(new ArrayList<>(hybridResult.missingKeywords))
                .recommendations(recommendations.toString())
                .requestId(requestId)
                .build();
    }

    private AtsHybridResult computeHybridAts(String resumeContent, String jobDescription) {
        List<String> jdKeywords = extractKeywords(jobDescription);
        if (jdKeywords.isEmpty()) {
            return new AtsHybridResult(0, List.of());
        }

        String normalizedResume = normalize(resumeContent);
        List<String> missing = new ArrayList<>();
        int matched = 0;

        for (String keyword : jdKeywords) {
            if (normalizedResume.contains(keyword)) {
                matched++;
            } else {
                missing.add(keyword);
            }
        }

        int keywordScore = Math.round((matched * 100.0f) / jdKeywords.size());
        return new AtsHybridResult(keywordScore, missing);
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> stopWords = Set.of(
                "the", "and", "for", "with", "from", "that", "this", "your", "you", "will", "have", "has",
                "our", "are", "not", "but", "all", "any", "job", "role", "work", "team", "years", "year",
                "using", "use", "into", "out", "can", "should", "must", "etc", "per", "who", "how", "what",
                "where", "when", "why", "their", "them", "they", "was", "were", "been", "being", "also");

        String[] rawTokens = normalize(text).split("\\s+");
        Map<String, Integer> frequency = new HashMap<>();

        for (String token : rawTokens) {
            if (token.length() < 3 || stopWords.contains(token)) {
                continue;
            }
            frequency.merge(token, 1, Integer::sum);
        }

        return frequency.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(20)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9#+. ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private AtsStandaloneResult computeStandaloneAts(String resumeContent) {
        String lower = resumeContent == null ? "" : resumeContent.toLowerCase(Locale.ROOT);
        int score = 0;
        List<String> missingItems = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        String[][] sectionChecks = {
                {"email", "phone", "@", "linkedin"},
                {"experience", "employment", "work history", "professional experience"},
                {"education", "degree", "university", "college", "bachelor", "master"},
                {"skills", "technical skills", "core competencies"},
                {"summary", "objective", "profile", "about"}
        };
        String[] sectionNames = {
                "Contact information",
                "Work experience",
                "Education",
                "Skills",
                "Summary/Objective"
        };

        for (int i = 0; i < sectionChecks.length; i++) {
            boolean present = false;
            for (String token : sectionChecks[i]) {
                if (lower.contains(token)) {
                    present = true;
                    break;
                }
            }
            if (present) {
                score += 12;
            } else {
                missingItems.add(sectionNames[i]);
            }
        }

        String[][] optionalChecks = {
                {"project", "projects"},
                {"certification", "certified"},
                {"achievement", "award", "accomplishment"}
        };
        String[] optionalNames = {"Projects", "Certifications", "Achievements"};
        for (int i = 0; i < optionalChecks.length; i++) {
            boolean present = false;
            for (String token : optionalChecks[i]) {
                if (lower.contains(token)) {
                    present = true;
                    break;
                }
            }
            if (present) {
                score += 6;
            } else {
                missingItems.add(optionalNames[i]);
            }
        }

        String[] actionVerbs = {
                "led", "managed", "developed", "designed", "implemented", "created",
                "improved", "optimized", "increased", "reduced", "delivered", "built"
        };
        int verbCount = 0;
        for (String verb : actionVerbs) {
            if (lower.contains(verb)) {
                verbCount++;
            }
        }
        score += Math.min(12, verbCount * 2);
        if (verbCount < 4) {
            suggestions.add("Use stronger action verbs (for example: led, built, optimized, delivered).");
        }

        boolean hasMetrics = resumeContent != null && (
                resumeContent.matches("(?s).*\\d+%.*") ||
                resumeContent.matches("(?s).*\\$\\s*\\d+.*") ||
                resumeContent.matches("(?s).*\\d+\\+.*"));
        if (hasMetrics) {
            score += 10;
        } else {
            missingItems.add("Quantified achievements");
            suggestions.add("Add measurable outcomes (percentages, counts, revenue, time saved).");
        }

        int normalizedScore = Math.max(20, Math.min(100, score));

        if (missingItems.contains("Skills")) {
            suggestions.add("Add a dedicated skills section with tools, languages, and frameworks.");
        }
        if (missingItems.contains("Work experience")) {
            suggestions.add("Include a clear work experience section with role, company, dates, and impact bullets.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Good baseline ATS structure. Tailor keywords for each job description before applying.");
        }

        return new AtsStandaloneResult(normalizedScore, missingItems, String.join("\n", suggestions));
    }

    private record AtsHybridResult(int keywordScore, List<String> missingKeywords) {
    }

    private record AtsStandaloneResult(int score, List<String> missingItems, String recommendations) {
    }

    private String cleanJsonString(String jsonText) {
        if (jsonText == null) {
            return "";
        }
        String cleaned = jsonText.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private ResumeExtractResponse normalizeResumeExtract(ResumeExtractResponse response) {
        if (response == null) {
            return ResumeExtractResponse.builder()
                    .skills(List.of())
                    .roles(List.of())
                    .keywords(List.of())
                    .experience("")
                    .build();
        }
        response.setSkills(normalizeList(response.getSkills()));
        response.setRoles(normalizeList(response.getRoles()));
        response.setKeywords(normalizeList(response.getKeywords()));
        if (response.getExperience() != null) {
            response.setExperience(response.getExperience().trim());
        }
        return response;
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private int estimateTokens(String text) {
        // Rough estimate: ~4 chars per token
        return text == null ? 0 : text.length() / 4;
    }

    private AiProviderResult executeWithTimeout(java.util.concurrent.Callable<AiProviderResult> task, String operationName)
            throws Exception {
        long timeoutSeconds = Math.max(2, aiRequestTimeoutSeconds);
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException ex) {
            throw new AiServiceException(operationName + " timed out after " + timeoutSeconds + " seconds", ex);
        } catch (java.util.concurrent.ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception e) {
                throw e;
            }
            throw new AiServiceException("AI execution failed", ex);
        }
    }

    private AIHistoryResponse mapToHistoryResponse(AiRequest req) {
        return AIHistoryResponse.builder()
                .requestId(req.getRequestId())
                .userId(req.getUserId())
                .resumeId(req.getResumeId())
                .requestType(req.getRequestType())
                .inputPrompt(req.getInputPrompt())
                .aiResponse(req.getAiResponse())
                .model(req.getModel())
                .status(req.getStatus())
                .tokensUsed(req.getTokensUsed())
                .createdAt(req.getCreatedAt())
                .completedAt(req.getCompletedAt())
                .build();
    }
}
