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
import com.resumeai.ai.util.PromptBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final AiRequestRepository repository;
    private static final String NVIDIA_MODEL = "z-ai/glm-4.7";
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${ai.quota.free.monthly-calls:5}")
    private int freeMonthlyCallsLimit;

    @Value("${ai.quota.free.monthly-ats:3}")
    private int freeMonthlyAtsLimit;

    @Value("${rabbitmq.exchange:notification.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.ai:ai.completed}")
    private String aiRoutingKey;

    // ── Public API methods ──────────────────────────────────────────────────

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
        enforceAtsQuota(userId);
        String prompt = PromptBuilder.buildAtsPrompt(req.getResumeContent(), req.getJobDescription());

        AiRequest record = saveQueued(userId, resumeId, RequestType.ATS, prompt);

        try {
            String rawText = extractNvidiaContent(callAi(prompt, record));
            ATSResponse atsResponse = parseAtsResponse(rawText, req.getResumeContent(), req.getJobDescription());
            markCompleted(record, rawText, estimateTokens(rawText));
            atsResponse.setRequestId(record.getRequestId());
            return atsResponse;
        } catch (QuotaExceededException ex) {
            log.warn("AI quota exceeded. Falling back to hybrid ATS scoring.");
            AtsHybridResult hybridResult = computeHybridAts(req.getResumeContent(), req.getJobDescription());
            ATSResponse atsResponse = ATSResponse.builder()
                    .score(hybridResult.keywordScore)
                    .missingKeywords(new ArrayList<>(hybridResult.missingKeywords))
                    .recommendations("AI TEMPORARILY UNAVAILABLE")
                    .requestId(record.getRequestId())
                    .build();
            markCompleted(record, "AI TEMPORARILY UNAVAILABLE", 0);
            return atsResponse;
        } catch (Exception ex) {
            markFailed(record);
            AtsHybridResult hybridResult = computeHybridAts(req.getResumeContent(), req.getJobDescription());
            ATSResponse atsResponse = ATSResponse.builder()
                    .score(hybridResult.keywordScore)
                    .missingKeywords(new ArrayList<>(hybridResult.missingKeywords))
                    .recommendations("AI TEMPORARILY UNAVAILABLE")
                    .requestId(record.getRequestId())
                    .build();
            return atsResponse;
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
    public ResumeExtractResponse extractResumeData(ResumeExtractRequest request) {
        if (request == null) {
            throw new RuntimeException("Request is NULL");
        }

        String resumeText = request.getResumeText() == null ? "" : request.getResumeText();
        if (resumeText.trim().isEmpty()) {
            throw new RuntimeException("PDF TEXT EMPTY");
        }

        System.out.println("EXTRACTED TEXT LENGTH: " + resumeText.length());

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
                    .model(NVIDIA_MODEL)
                    .build();
        }

        try {
            String rawText = extractNvidiaContent(callAi(prompt, record));
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
            String rawText = extractNvidiaContent(callAi(prompt, record));
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
                .collect(Collectors.toList());
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

    // ── Private helpers ─────────────────────────────────────────────────────

    private AIResponse executeAndSave(Long userId, Long resumeId, RequestType type, String prompt) {
        AiRequest record = saveQueued(userId, resumeId, type, prompt);
        try {
            String text = extractNvidiaContent(callAi(prompt, record));
            int tokens = estimateTokens(text);
            markCompleted(record, text, tokens);
            return AIResponse.builder()
                    .text(text)
                    .model(record.getModel())
                    .tokensUsed(tokens)
                    .requestId(record.getRequestId())
                    .build();
        } catch (Exception ex) {
            markFailed(record);
            return AIResponse.builder()
                    .text("AI TEMPORARILY UNAVAILABLE")
                    .model(NVIDIA_MODEL)
                    .tokensUsed(0)
                    .requestId(record.getRequestId())
                    .build();
        }
    }

    /**
     * NVIDIA NIM provider (glm-4.7).
     */
    private String callAi(String prompt, AiRequest record) {
        String response = callNvidiaAI(prompt);
        record.setModel(NVIDIA_MODEL);
        return response;
    }

    private String extractNvidiaContent(String raw) {
        if (raw == null || !raw.contains("choices")) {
            throw new RuntimeException("Invalid AI response");
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode() && !content.asText("").isBlank()) {
                return content.asText();
            }
        } catch (Exception ignored) {
            // Fall through to raw output
        }
        return raw;
    }

    public String callNvidiaAI(String prompt) {
        try {
            String url = "https://integrate.api.nvidia.com/v1/chat/completions";
            String apiKey = System.getenv("NVIDIA_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new AiServiceException("NVIDIA_API_KEY is not configured");
            }

            System.out.println("NVIDIA_API_KEY=" + (apiKey.length() > 6 ? apiKey.substring(0, 6) + "***" : "***"));

                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setBearerAuth(apiKey);
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

                Map<String, Object> body = new HashMap<>();
                body.put("model", "z-ai/glm-4.7");
                List<Map<String, String>> messages = new ArrayList<>();
                Map<String, String> msg = new HashMap<>();
                msg.put("role", "user");
                msg.put("content", prompt);
                messages.add(msg);
                body.put("messages", messages);
                body.put("temperature", 0.3);
                body.put("max_tokens", 2000);

                org.springframework.http.HttpEntity<Map<String, Object>> request =
                    new org.springframework.http.HttpEntity<>(body, headers);

                org.springframework.http.ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

                System.out.println("NVIDIA RESPONSE: " + response.getBody());

            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new AiServiceException("NVIDIA AI returned empty response");
            }

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("NVIDIA AI FAILED: " + e.getMessage(), e);
        }
    }

    @Transactional
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

    @Transactional
    private void markCompleted(AiRequest req, String response, int tokens) {
        req.setAiResponse(response);
        req.setTokensUsed(tokens);
        req.setStatus(RequestStatus.COMPLETED);
        req.setCompletedAt(LocalDateTime.now());
        repository.save(req);

        // Publish ai.completed event — notification-service will create a notification
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(req.getUserId())
                    .subject("AI Enhancement Complete — ResumeAI")
                    .message("Your AI " + req.getRequestType().name().toLowerCase()
                            + " request has been completed successfully.")
                    .build();
            rabbitTemplate.convertAndSend(exchange, aiRoutingKey, event);
            log.debug("Published ai.completed event for userId={}", req.getUserId());
        } catch (Exception ex) {
            log.warn("Failed to publish ai.completed event for userId={}: {}", req.getUserId(), ex.getMessage());
        }
    }

    @Transactional
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
        // Strip markdown code fences if the AI wraps response in ```json ... ```
        String cleaned = cleanJsonString(rawText);
        AtsHybridResult hybridResult = computeHybridAts(resumeContent, jobDescription);
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            int aiScore = node.path("score").asInt(0);
            int finalScore = Math.max(0, Math.min(100, Math.round((hybridResult.keywordScore * 0.7f) + (aiScore * 0.3f))));

            List<String> aiMissingKeywords = Arrays.asList(
                objectMapper.convertValue(node.path("missingKeywords"), String[].class)
            );
            List<String> keywords = new ArrayList<>(hybridResult.missingKeywords);
            for (String keyword : aiMissingKeywords) {
                if (keyword != null && !keyword.isBlank() && !keywords.contains(keyword)) {
                    keywords.add(keyword);
                }
            }
            String recommendations = node.path("recommendations").asText("");
            return ATSResponse.builder()
                    .score(finalScore)
                    .missingKeywords(keywords)
                    .recommendations(mergeRecommendations(recommendations, hybridResult))
                    .build();
        } catch (JsonProcessingException ex) {
            log.warn("Could not parse ATS JSON response, returning raw text as recommendations");
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

    private record AtsHybridResult(int keywordScore, List<String> missingKeywords) {
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
                .collect(Collectors.toList());
    }

    private int estimateTokens(String text) {
        // Rough estimate: ~4 chars per token
        return text == null ? 0 : text.length() / 4;
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
