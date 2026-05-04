package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.client.ResumeClient;
import com.resumeai.jobmatch.client.SectionClient;
import com.resumeai.jobmatch.client.AiServiceClient;
import com.resumeai.jobmatch.dto.AiServiceResponse;
import com.resumeai.jobmatch.dto.AnalysisResponse;
import com.resumeai.jobmatch.dto.GeminiExtractionResponse;
import com.resumeai.jobmatch.dto.JobResponse;
import com.resumeai.jobmatch.dto.MatchResponse;
import com.resumeai.jobmatch.dto.NotificationEvent;
import com.resumeai.jobmatch.dto.ResumeDTO;
import com.resumeai.jobmatch.dto.ResumeExtractRequest;
import com.resumeai.jobmatch.dto.ResumeExtractResponse;
import com.resumeai.jobmatch.dto.ResumeStructuredData;
import com.resumeai.jobmatch.dto.SectionDTO;
import com.resumeai.jobmatch.dto.MissingSkillsRequest;
import com.resumeai.jobmatch.dto.MissingSkillsResponse;
import com.resumeai.jobmatch.entity.Job;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import com.resumeai.jobmatch.repository.JobRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchService {

    private static final Pattern YEARS_PATTERN = Pattern.compile("(\\d+)\\s*\\+?\\s*(year|years|yr|yrs)", Pattern.CASE_INSENSITIVE);

    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final ResumeClient resumeClient;
    private final SectionClient sectionClient;
    private final RabbitTemplate rabbitTemplate;
    private final PdfParserService pdfParserService;
    private final ResumeStructuringService resumeStructuringService;
    private final AiServiceClient aiServiceClient;
    private final AdzunaService adzunaService;
    private final TheirStackService theirStackService;

    @Value("${rabbitmq.exchange:notification.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.job:job.match}")
    private String jobRoutingKey;

    @Transactional(readOnly = true)
    public List<JobResponse> fetchJobs(String keyword) {
        List<Job> jobs = (keyword != null && !keyword.isBlank())
                ? jobRepository.findByTitleContainingIgnoreCase(keyword)
                : jobRepository.findAll();

        return jobs.stream().map(this::toJobResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<JobResponse> fetchSavedJobs() {
        return jobRepository.findBySourceOrderByCreatedAtDesc(com.resumeai.jobmatch.entity.JobSource.ADZUNA)
                .stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Transactional
    public List<MatchResponse> analyzeAndMatch(
            MultipartFile file,
            Long resumeId,
            Long userId,
            String jobTitle,
            String location) {
        return analyzeAndMatchDetailed(file, resumeId, userId, jobTitle, location).getMatches();
    }

    @Transactional
    public AnalysisResponse analyzeAndMatchDetailed(
            MultipartFile file,
            Long resumeId,
            Long userId,
            String jobTitle,
            String location) {

        if ((file == null || file.isEmpty()) && resumeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either resumeId or PDF file is required");
        }

        String rawResumeText = (file != null && !file.isEmpty())
                ? pdfParserService.extractTextFromPdf(file)
                : extractRawResumeTextFromDb(resumeId);
        
        // STEP 3: Null check for raw resume text
        if (rawResumeText == null) {
            log.error("ERROR: rawResumeText is null after extraction");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not extract resume text");
        }

        ResumeStructuredData structuredResume = resumeStructuringService.fromRawText(rawResumeText);
        // STEP 3: Null check for structured resume
        if (structuredResume == null) {
            log.error("ERROR: structuredResume is null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not structure resume data");
        }
        
        if (structuredResume.getNormalizedText() == null || structuredResume.getNormalizedText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume text is empty. Please upload a valid resume");
        }

        Long effectiveResumeId = resumeId != null ? resumeId : 0L;
        
        log.info("STEP 3: Resume text extracted and structured successfully. Length: {}", structuredResume.getNormalizedText().length());

        // STEP 4: Call AI service (NVIDIA NIM via ai-service)
        System.out.println("STEP 3: Calling AI");
        AiServiceResponse<ResumeExtractResponse> aiResponse = aiServiceClient.extractResume(
            ResumeExtractRequest.builder()
                .userId(userId)
                .resumeId(effectiveResumeId)
                .resumeText(structuredResume.getNormalizedText())
                .build());

        if (aiResponse != null && "failed".equalsIgnoreCase(aiResponse.getStatus())) {
            log.warn("AI resume extraction failed: {}", aiResponse.getMessage());
        }

        ResumeExtractResponse resumeExtract = normalizeResumeExtractResponse(aiResponse, structuredResume, rawResumeText, queryTitleFromResume(structuredResume, jobTitle));

        System.out.println("STEP 4: Parsing response");

        GeminiExtractionResponse extracted = GeminiExtractionResponse.builder()
            .skills(resumeExtract.getSkills())
            .roles(resumeExtract.getRoles())
            .experienceLevel(resumeExtract.getExperience())
            .keywords(resumeExtract.getKeywords())
            .experienceLevel(resumeExtract.getExperience())
            .build();

        List<String> extractedSkills = extracted.getSkills() != null ? extracted.getSkills() : List.of();
        List<String> extractedRoles = extracted.getRoles() != null ? extracted.getRoles() : List.of();

        // 🔴 STEP 7: IMPROVE JOB SEARCH QUERY - combine roles + top skills
        String improvedQuery = buildImprovedSearchQuery(extractedRoles, extractedSkills);
        String queryTitle = (jobTitle != null && !jobTitle.isBlank())
            ? jobTitle.trim()
            : firstNonBlank(
                extractedRoles.isEmpty() ? "" : extractedRoles.get(0),
                extractedSkills.isEmpty() ? "" : extractedSkills.get(0),
                improvedQuery,
                "software developer");
        
        log.info("🔵 STEP 5: Job search query: {}", queryTitle);
        
        // STEP 5: Try Adzuna API first, then fallback to TheirStack
        List<Map<String, Object>> fetchedJobs = new ArrayList<>();
        
        try {
            log.info("[ADZUNA] Attempting to fetch jobs from Adzuna API...");
            fetchedJobs = adzunaService.fetchJobs(queryTitle);
            log.info("[ADZUNA] Adzuna API returned {} jobs", fetchedJobs != null ? fetchedJobs.size() : 0);
            
            if (fetchedJobs != null && !fetchedJobs.isEmpty()) {
                log.info("✓ Using Adzuna API results ({} jobs)", fetchedJobs.size());
            } else {
                log.warn("[FALLBACK] Adzuna returned 0 jobs, trying TheirStack API...");
                try {
                    fetchedJobs = theirStackService.searchJobs(queryTitle, extractedSkills, location);
                    log.info("[FALLBACK] TheirStack API returned {} jobs", fetchedJobs != null ? fetchedJobs.size() : 0);
                } catch (Exception theirStackErr) {
                    log.error("[FALLBACK] TheirStack API also failed: {}", theirStackErr.getMessage());
                    fetchedJobs = new ArrayList<>();
                }
            }
        } catch (Exception adzunaErr) {
            log.warn("[FALLBACK] Adzuna API failed: {}, trying TheirStack as fallback", adzunaErr.getMessage());
            try {
                fetchedJobs = theirStackService.searchJobs(queryTitle, extractedSkills, location);
                log.info("[FALLBACK] TheirStack API returned {} jobs", fetchedJobs != null ? fetchedJobs.size() : 0);
            } catch (Exception theirStackErr) {
                log.error("[FALLBACK] TheirStack API also failed: {}", theirStackErr.getMessage());
                fetchedJobs = new ArrayList<>();
            }
        }
        
        // STEP 3: Null check for fetchedJobs
        if (fetchedJobs == null) {
            log.warn("STEP 3: fetchedJobs is null, initializing empty list");
            fetchedJobs = new ArrayList<>();
        }

        if (fetchedJobs.isEmpty()) {
            log.warn("⚠ No jobs found from any API. Returning empty results.");
            AnalysisResponse.ExtractedData extractedData = AnalysisResponse.ExtractedData.builder()
                .skills(extracted.getSkills())
                .roles(extracted.getRoles())
                .keywords(extracted.getKeywords())
                .experienceLevel(extracted.getExperienceLevel())
                .summary(structuredResume.getSummary())
                .build();

            return AnalysisResponse.builder()
                .extractedData(extractedData)
                .matches(new ArrayList<>())
                .jobs(new ArrayList<>())
                .totalMatches(0)
                .build();
        }
        
        log.info("STEP 10: Processing {} jobs for matching", fetchedJobs.size());

        if (userId != null) {
            jobMatchRepository.deleteByUserIdAndResumeId(userId, effectiveResumeId);
        }

        List<MatchResponse> responses = new ArrayList<>();

        for (Map<String, Object> jobData : fetchedJobs) {
            Long externalJobId = firstNonNullLong(
                    readLong(jobData, "job_id"),
                    readLong(jobData, "id"));

            String title = firstNonBlank(
                    readString(jobData, "job_title"),
                    readString(jobData, "title"),
                    "Software Developer");

            String description = firstNonBlank(
                    readString(jobData, "description"),
                    readString(jobData, "job_description"),
                    "");

            String company = firstNonBlank(
                    readString(jobData, "company"),
                    readString(jobData, "company_name"),
                    "Unknown Company");

            String resolvedLocation = firstNonBlank(
                    readString(jobData, "location"),
                    readString(jobData, "job_location"),
                    location,
                    "Remote");

            String applyUrl = firstNonBlank(
                    readString(jobData, "url"),
                    readString(jobData, "job_url"),
                    readString(jobData, "apply_url"),
                    "");

            String source = firstNonBlank(
                    readString(jobData, "source"),
                    "ADZUNA");

            double matchScore = computeAdvancedScore(extracted, structuredResume, title, description);
            log.debug("STEP 10: Job '{}' scored: {}", title, matchScore);
            
            // STEP 4: Wrap missing skills analysis with try-catch
            MissingSkillsResponse analysis = MissingSkillsResponse.builder()
                    .missingSkills("")
                    .recommendations("")
                    .build();
            try {
                AiServiceResponse<MissingSkillsResponse> missingSkills = aiServiceClient.analyzeMissingSkills(
                        MissingSkillsRequest.builder()
                                .userId(userId)
                                .resumeId(resumeId)
                                .resumeText(structuredResume.getNormalizedText())
                                .jobDescription(description)
                                .build());
                if (missingSkills != null && missingSkills.getData() != null) {
                    analysis = missingSkills.getData();
                }
                log.debug("STEP 10: Missing skills analysis completed for '{}'", title);
            } catch (Exception e) {
                log.warn("STEP 4: Failed to analyze missing skills for '{}': {}", title, e.getMessage());
            }

            JobMatch match = JobMatch.builder()
                    .userId(userId != null ? userId : 0L)
                    .resumeId(effectiveResumeId)
                    .jobId(externalJobId != null ? externalJobId : 0L)
                    .jobTitle(title)
                    .company(company)
                    .location(resolvedLocation)
                    .applyUrl(applyUrl)
                    .jobDescription(description)
                    .matchScore(matchScore)
                    .missingSkills(analysis.getMissingSkills() == null ? "" : analysis.getMissingSkills())
                    .recommendations(analysis.getRecommendations() == null ? "" : analysis.getRecommendations())
                    .source(source)
                    .isBookmarked(false)
                    .build();
            
            log.debug("STEP 10: Match created: title={}, score={}, source={}, userId={}", title, matchScore, source, userId);

            if (userId != null) {
                match = jobMatchRepository.save(match);
                log.debug("STEP 10: Match persisted to DB with matchId: {}", match.getMatchId());
            }

            responses.add(toMatchResponse(match));
            log.debug("STEP 10: Match added to response list. Total responses: {}", responses.size());
        }

        responses.sort(Comparator.comparingDouble(MatchResponse::getMatchScore).reversed());
        log.info("\n===== ANALYSIS COMPLETE =====");
        log.info("STEP 10: Total matches created: {}", responses.size());
        log.info("STEP 10: Top match score: {}", responses.stream().mapToDouble(MatchResponse::getMatchScore).max().orElse(0));
        log.info("STEP 10: Resume: {} | userId: {} | location: {}", rawResumeText.length() + " chars", userId, location);
        log.info("==============================\n");
        
        if (userId != null) {
            publishMatchNotification(userId, responses.size());
        }
        AnalysisResponse.ExtractedData extractedData = AnalysisResponse.ExtractedData.builder()
            .skills(extracted.getSkills())
            .roles(extracted.getRoles())
            .keywords(extracted.getKeywords())
            .experienceLevel(extracted.getExperienceLevel())
            .summary(structuredResume.getSummary())
            .build();

        return AnalysisResponse.builder()
            .extractedData(extractedData)
            .matches(responses)
            .jobs(new ArrayList<>(fetchedJobs))
            .totalMatches(responses.size())
            .build();
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getRankedJobs(Long userId) {
        // STEP 5: Null check for userId
        if (userId == null || userId <= 0) {
            log.warn("STEP 5: getRankedJobs called with invalid userId: {}", userId);
            return new ArrayList<>();
        }
        
        List<JobMatch> matches = jobMatchRepository.findByUserIdOrderByMatchScoreDescCreatedAtDesc(userId);
        // STEP 5: Null check for repository result
        if (matches == null) {
            log.warn("STEP 5: Repository returned null for userId: {}", userId);
            return new ArrayList<>();
        }
        
        log.info("STEP 10: getRankedJobs retrieved {} matches for userId {}", matches.size(), userId);
        return matches.stream()
                .map(this::toMatchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getTopMatches(Long userId, int limit) {
        // STEP 5: Null check for userId and limit
        if (userId == null || userId <= 0) {
            log.warn("STEP 5: getTopMatches called with invalid userId: {}", userId);
            return new ArrayList<>();
        }
        
        if (limit <= 0) {
            limit = 10; // Default limit
        }
        
        List<JobMatch> matches = jobMatchRepository.findByUserIdOrderByMatchScoreDescCreatedAtDesc(userId);
        // STEP 5: Null check for repository result
        if (matches == null) {
            log.warn("STEP 5: Repository returned null for userId: {}", userId);
            return new ArrayList<>();
        }
        
        log.info("STEP 10: getTopMatches retrieving top {} from {} total matches for userId {}", limit, matches.size(), userId);
        return matches.stream()
                .limit(Math.max(limit, 1))
                .map(this::toMatchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesForUser(Long userId) {
        return getRankedJobs(userId);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesForResume(Long resumeId) {
        // STEP 3: Null check for resumeId
        if (resumeId == null || resumeId <= 0) {
            log.warn("STEP 3: getMatchesForResume called with invalid resumeId: {}", resumeId);
            return new ArrayList<>();
        }
        
        List<JobMatch> matches = jobMatchRepository.findByResumeId(resumeId);
        // STEP 3: Null check for repository result
        if (matches == null) {
            log.warn("STEP 3: Repository returned null for resumeId: {}", resumeId);
            return new ArrayList<>();
        }
        
        log.info("STEP 10: getMatchesForResume retrieved {} matches for resumeId {}", matches.size(), resumeId);
        return matches.stream()
                .map(this::toMatchResponse)
                .sorted(Comparator.comparing(MatchResponse::getMatchScore).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatchById(UUID matchId) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + matchId));
        return toMatchResponse(match);
    }

    @Transactional
    public MatchResponse updateBookmark(UUID matchId, boolean bookmarked) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + matchId));
        match.setBookmarked(bookmarked);
        return toMatchResponse(jobMatchRepository.save(match));
    }

    @Transactional
    public MatchResponse toggleBookmark(UUID matchId) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + matchId));
        match.setBookmarked(!match.isBookmarked());
        return toMatchResponse(jobMatchRepository.save(match));
    }

    @Transactional
    public void deleteMatch(UUID matchId) {
        if (!jobMatchRepository.existsById(matchId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + matchId);
        }
        jobMatchRepository.deleteById(matchId);
    }

    @Transactional
    public List<MatchResponse> matchResumeWithJobs(Long userId, Long resumeId, String customJobTitle, String customJobDesc) {
        return analyzeAndMatch(null, resumeId, userId, customJobTitle, null);
    }

    private double computeAdvancedScore(
            GeminiExtractionResponse extracted,
            ResumeStructuredData structuredResume,
            String jobTitle,
            String jobDescription) {

        List<String> skills = extracted.getSkills() == null ? List.of() : extracted.getSkills();
        List<String> roles = extracted.getRoles() == null ? List.of() : extracted.getRoles();
        
        // 🔴 STEP 8: Use new improved score calculation
        return calculateImprovedScore(skills, List.of(jobDescription), roles, jobTitle);
    }

    private double calculateRatioScore(List<String> tokens, String targetText, double weight) {
        if (tokens == null || tokens.isEmpty()) {
            return weight * 0.5;
        }

        long matched = tokens.stream()
                .filter(token -> token != null && !token.isBlank())
                .filter(token -> targetText.contains(token.toLowerCase(Locale.ROOT)))
                .count();

        return ((double) matched / tokens.size()) * weight;
    }

    private double calculateExperienceScore(String experienceLevel, List<String> resumeExperience, String descriptionLower) {
        int resumeYears = Math.max(
                extractYears(experienceLevel),
                extractYears(String.join(" ", resumeExperience == null ? List.of() : resumeExperience)));
        int jobYears = extractYears(descriptionLower);

        if (resumeYears <= 0 || jobYears <= 0) {
            return 5.0;
        }

        int diff = Math.abs(jobYears - resumeYears);
        if (diff == 0) {
            return 10.0;
        }
        if (diff <= 1) {
            return 8.0;
        }
        if (diff <= 3) {
            return 6.0;
        }
        return 3.0;
    }

    private int extractYears(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        Matcher matcher = YEARS_PATTERN.matcher(text);
        int max = 0;
        while (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));
                if (years > max) {
                    max = years;
                }
            } catch (NumberFormatException ignored) {
                // No-op
            }
        }
        return max;
    }

    private String extractRawResumeTextFromDb(Long resumeId) {
        if (resumeId == null) {
            return "";
        }

        StringBuilder combined = new StringBuilder();
        boolean resumeFound = false;

        try {
            ResumeDTO resume = resumeClient.getResumeById(resumeId);
            if (resume != null) {
                resumeFound = true;
                appendValue(combined, resume.getTitle());
                appendValue(combined, resume.getTargetJobTitle());
                appendValue(combined, resume.getSectionsJson());
            }
        } catch (Exception ex) {
            log.warn("Could not fetch resumeId={} from resume-service: {}", resumeId, ex.getMessage());
        }

        try {
            List<SectionDTO> sections = sectionClient.getSectionsByResumeId(resumeId);
            if (sections != null) {
                for (SectionDTO section : sections) {
                    appendValue(combined, section.getTitle());
                    appendValue(combined, section.getContent());
                }
            }
        } catch (Exception ex) {
            log.warn("Could not fetch sections for resumeId={}: {}", resumeId, ex.getMessage());
        }

        if (!resumeFound) {
            throw new RuntimeException("Resume not found");
        }

        String cleaned = resumeStructuringService.normalizeText(combined.toString());
        if (cleaned.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected resume has no analyzable content");
        }
        return cleaned;
    }

    private List<String> coalesceSkills(List<String> extractedSkills, List<String> parsedSkills) {
        Set<String> merged = new HashSet<>();
        if (extractedSkills != null) {
            merged.addAll(extractedSkills.stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .map(skill -> skill.trim().toLowerCase(Locale.ROOT))
                    .toList());
        }
        if (parsedSkills != null) {
            merged.addAll(parsedSkills.stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .map(skill -> skill.trim().toLowerCase(Locale.ROOT))
                    .toList());
        }

        if (merged.isEmpty()) {
            return List.of("software", "developer");
        }
        return new ArrayList<>(merged);
    }

    private String readString(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key) || payload.get(key) == null) {
            return "";
        }
        Object value = payload.get(key);
        if (value instanceof String s) {
            return s.trim();
        }
        return String.valueOf(value).trim();
    }

    private Long readLong(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key) || payload.get(key) == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long firstNonNullLong(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void appendValue(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append(' ').append(value);
    }

    private MatchResponse toMatchResponse(JobMatch match) {
        return MatchResponse.builder()
                .matchId(match.getMatchId())
                .userId(match.getUserId())
                .resumeId(match.getResumeId())
.jobId(match.getJobId())
                .jobTitle(match.getJobTitle())
                .company(match.getCompany())
                .location(match.getLocation())
                .jobDescription(match.getJobDescription())
                .applyUrl(match.getApplyUrl())
                .source(match.getSource())
                .matchScore(match.getMatchScore())
                .missingSkills(match.getMissingSkills())
                .recommendation(match.getRecommendations())
                .isBookmarked(match.isBookmarked())
                .createdAt(match.getCreatedAt() != null ? match.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    private Set<String> parseSkills(String skillsText) {
        if (skillsText == null || skillsText.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(skillsText.split("[,;\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private JobResponse toJobResponse(Job job) {
        return JobResponse.builder()
                .jobId(job.getJobId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .description(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .source(job.getSource())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private void publishMatchNotification(Long userId, int totalMatches) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(userId)
                    .subject("New Job Matches Found - ResumeAI")
                    .message("We found " + totalMatches + " job match(es) for your resume.")
                    .build();
            rabbitTemplate.convertAndSend(exchange, jobRoutingKey, event);
        } catch (Exception ex) {
            log.warn("Failed to publish job.match event for userId={}: {}", userId, ex.getMessage());
        }
    }

    // 🔴 STEP 7: IMPROVED JOB SEARCH QUERY - combine roles + top skills
    private String buildImprovedSearchQuery(List<String> roles, List<String> skills) {
        StringBuilder query = new StringBuilder();
        
        // Add top roles
        if (roles != null && !roles.isEmpty()) {
            String topRoles = roles.stream()
                    .filter(r -> r != null && !r.isBlank())
                    .limit(2)
                    .collect(Collectors.joining(" "));
            if (!topRoles.isBlank()) {
                query.append(topRoles).append(" ");
            }
        }
        
        // Add top 3 skills
        if (skills != null && !skills.isEmpty()) {
            String topSkills = skills.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .limit(3)
                    .collect(Collectors.joining(" "));
            if (!topSkills.isBlank()) {
                query.append(topSkills);
            }
        }
        
        String result = query.toString().trim();
        log.info("🟢 Built improved search query: {}", result);
        return result.isEmpty() ? "software engineer" : result;
    }

    // 🔴 STEP 8: IMPROVED MATCH SCORE - use ratio formula
    private double calculateImprovedScore(
            List<String> resumeSkills, 
            List<String> jobDescription,
            List<String> roles,
            String jobTitle) {
        
        String descLower = jobDescription.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
        String titleLower = jobTitle == null ? "" : jobTitle.toLowerCase(Locale.ROOT);
        
        // 🔴 Skill matching: (matchedSkills / totalSkills) * 100
        int totalSkills = resumeSkills == null || resumeSkills.isEmpty() ? 1 : resumeSkills.size();
        long matchedSkills = resumeSkills == null ? 0 : resumeSkills.stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .filter(skill -> descLower.contains(skill.toLowerCase(Locale.ROOT)))
                .count();
        
        double skillScore = ((double) matchedSkills / totalSkills) * 100.0;
        
        // Role matching
        boolean roleMatched = roles != null && roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .anyMatch(role -> titleLower.contains(role.toLowerCase(Locale.ROOT)));
        
        double roleScore = roleMatched ? 20.0 : 5.0;
        
        // Final score: weighted combination
        double finalScore = (skillScore * 0.7) + (roleScore * 0.3);
        return Math.min(Math.round(finalScore * 100.0) / 100.0, 100.0);
    }

    private ResumeExtractResponse normalizeResumeExtractResponse(
            AiServiceResponse<ResumeExtractResponse> aiResponse,
            ResumeStructuredData structuredResume,
            String rawResumeText,
            String queryTitle) {

        ResumeExtractResponse aiExtract = aiResponse != null ? aiResponse.getData() : null;
        List<String> aiSkills = aiExtract != null && aiExtract.getSkills() != null ? aiExtract.getSkills() : List.of();
        List<String> aiRoles = aiExtract != null && aiExtract.getRoles() != null ? aiExtract.getRoles() : List.of();
        List<String> aiKeywords = aiExtract != null && aiExtract.getKeywords() != null ? aiExtract.getKeywords() : List.of();
        String aiExperience = aiExtract != null ? aiExtract.getExperience() : null;

        List<String> localSkills = structuredResume.getSkills() == null ? List.of() : structuredResume.getSkills();
        List<String> localRoles = deriveRoles(structuredResume, rawResumeText, queryTitle);
        List<String> localKeywords = deriveKeywords(structuredResume, rawResumeText, localRoles, localSkills);
        String localExperience = deriveExperienceLevel(rawResumeText, structuredResume);

        boolean useLocal = aiExtract == null
                || (aiSkills.isEmpty() && aiRoles.isEmpty() && aiKeywords.isEmpty())
                || aiExperience == null
                || aiExperience.isBlank()
                || "AI TEMPORARILY UNAVAILABLE".equalsIgnoreCase(aiExperience.trim());

        if (useLocal) {
            return ResumeExtractResponse.builder()
                    .skills(localSkills)
                    .roles(localRoles)
                    .keywords(localKeywords)
                    .experience(localExperience)
                    .build();
        }

        return ResumeExtractResponse.builder()
                .skills(coalesceTextList(aiSkills, localSkills))
                .roles(coalesceTextList(aiRoles, localRoles))
                .keywords(coalesceTextList(aiKeywords, localKeywords))
                .experience(aiExperience)
                .build();
    }

    private String queryTitleFromResume(ResumeStructuredData structuredResume, String fallback) {
        return fallback == null ? "" : fallback;
    }

    private List<String> deriveRoles(ResumeStructuredData structuredResume, String rawResumeText, String queryTitle) {
        Set<String> roles = new LinkedHashSet<>();
        if (structuredResume != null && structuredResume.getExperience() != null) {
            for (String entry : structuredResume.getExperience()) {
                if (entry == null) {
                    continue;
                }
                String lower = entry.toLowerCase(Locale.ROOT);
                if (lower.contains("developer")) roles.add("Software Developer");
                if (lower.contains("engineer")) roles.add("Software Engineer");
                if (lower.contains("frontend")) roles.add("Frontend Developer");
                if (lower.contains("backend")) roles.add("Backend Developer");
                if (lower.contains("full stack") || lower.contains("fullstack")) roles.add("Full Stack Developer");
                if (lower.contains("react")) roles.add("React Developer");
                if (lower.contains("data scientist")) roles.add("Data Scientist");
                if (lower.contains("analyst")) roles.add("Data Analyst");
                if (lower.contains("intern")) roles.add("Intern");
            }
        }

        String combined = (rawResumeText == null ? "" : rawResumeText) + " " + (queryTitle == null ? "" : queryTitle);
        String lowerCombined = combined.toLowerCase(Locale.ROOT);
        if (lowerCombined.contains("software engineer")) roles.add("Software Engineer");
        if (lowerCombined.contains("software developer")) roles.add("Software Developer");
        if (lowerCombined.contains("full stack")) roles.add("Full Stack Developer");
        if (lowerCombined.contains("frontend")) roles.add("Frontend Developer");
        if (lowerCombined.contains("backend")) roles.add("Backend Developer");
        if (lowerCombined.contains("react")) roles.add("React Developer");

        if (roles.isEmpty()) {
            roles.add("Software Developer");
        }

        return new ArrayList<>(roles);
    }

    private List<String> deriveKeywords(ResumeStructuredData structuredResume, String rawResumeText, List<String> roles, List<String> skills) {
        Set<String> keywords = new LinkedHashSet<>();
        if (skills != null) {
            keywords.addAll(skills.stream().filter(s -> s != null && !s.isBlank()).limit(10).toList());
        }
        if (roles != null) {
            keywords.addAll(roles.stream().filter(r -> r != null && !r.isBlank()).limit(5).toList());
        }
        if (structuredResume != null && structuredResume.getEducation() != null) {
            keywords.addAll(structuredResume.getEducation().stream().filter(e -> e != null && !e.isBlank()).limit(3).toList());
        }

        String text = rawResumeText == null ? "" : rawResumeText.toLowerCase(Locale.ROOT);
        for (String token : List.of("spring boot", "microservices", "rest api", "postgresql", "mysql", "docker", "aws", "kubernetes", "typescript", "javascript", "java", "react")) {
            if (text.contains(token)) {
                keywords.add(token);
            }
        }

        return new ArrayList<>(keywords);
    }

    private String deriveExperienceLevel(String rawResumeText, ResumeStructuredData structuredResume) {
        int years = extractYears(rawResumeText);
        if (years <= 0 && structuredResume != null) {
            years = extractYears(String.join(" ", structuredResume.getExperience() == null ? List.of() : structuredResume.getExperience()));
        }
        if (years >= 5) {
            return "5+ years";
        }
        if (years >= 3) {
            return "3-5 years";
        }
        if (years >= 1) {
            return "1-3 years";
        }
        return "Entry-level";
    }

    private List<String> coalesceTextList(List<String> primary, List<String> fallback) {
        Set<String> merged = new LinkedHashSet<>();
        if (primary != null) {
            merged.addAll(primary.stream().filter(value -> value != null && !value.isBlank()).toList());
        }
        if (fallback != null) {
            merged.addAll(fallback.stream().filter(value -> value != null && !value.isBlank()).toList());
        }
        return new ArrayList<>(merged);
    }
}
