package com.resumeai.resume.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.resumeai.resume.client.SectionServiceClient;
import com.resumeai.resume.dto.ResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeStatus;
import com.resumeai.resume.exception.InvalidInputException;
import com.resumeai.resume.exception.ResumeNotFoundException;
import com.resumeai.resume.repository.ResumeRepository;
import com.resumeai.resume.service.ResumeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

    private final ResumeRepository resumeRepository;
    private final GeminiAtsClient geminiAtsClient;
    private final SectionServiceClient sectionServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResumeResponse createResume(Long authenticatedUserId, ResumeRequest request, String authHeader, String userPlan) {
        requireAuthenticatedUser(authenticatedUserId);
        enforceFreePlanLimit(authenticatedUserId, authHeader, userPlan);

        Resume resume = Resume.builder()
                .userId(authenticatedUserId)
                .title(request.getTitle())
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .location(request.getLocation())
                .targetJobTitle(request.getTargetJobTitle())
                .templateId(request.getTemplateId())
                .language(request.getLanguage() != null && !request.getLanguage().isBlank() ? request.getLanguage()
                        : "English")
            .summary(request.getSummary())
            .skills(request.getSkills())
            .experience(request.getExperience())
            .education(request.getEducation())
            .projects(request.getProjects())
                .sectionsJson(request.getSectionsJson())
                .status(ResumeStatus.DRAFT)
                .atsScore(0.0)
                .isPublic(Boolean.FALSE)
                .viewCount(0L)
                .build();

        Resume saved = resumeRepository.save(resume);
        log.info("Created resume {} for user {}", saved.getResumeId(), authenticatedUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(Long resumeId, Long requesterUserId) {
        Resume resume = getResumeEntityById(resumeId);
        if (!Boolean.TRUE.equals(resume.getIsPublic())) {
            requireOwner(resume, requesterUserId);
        }
        return mapToResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesByUser(Long userId, Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        if (!userId.equals(requesterUserId)) {
            throw new InvalidInputException("Access denied for requested user resumes");
        }

        return resumeRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ResumeResponse updateResume(Long resumeId, Long requesterUserId, ResumeRequest request) {
        Resume existing = getResumeEntityById(resumeId);
        requireOwner(existing, requesterUserId);

        existing.setTitle(request.getTitle());
        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setLocation(request.getLocation());
        existing.setTargetJobTitle(request.getTargetJobTitle());
        existing.setTemplateId(request.getTemplateId());
        existing.setLanguage(request.getLanguage());
        existing.setSummary(request.getSummary());
        existing.setSkills(request.getSkills());
        existing.setExperience(request.getExperience());
        existing.setEducation(request.getEducation());
        existing.setProjects(request.getProjects());
        existing.setSectionsJson(request.getSectionsJson());

        Resume saved = resumeRepository.save(existing);
        log.info("Updated resume {} by user {}", resumeId, requesterUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteResume(Long resumeId, Long requesterUserId) {
        Resume existing = getResumeEntityById(resumeId);
        requireOwner(existing, requesterUserId);

        sectionServiceClient.deleteAllSections(resumeId, requesterUserId);
        resumeRepository.delete(existing);
        log.info("Deleted resume {} by user {}", resumeId, requesterUserId);
    }

    @Override
    @Transactional
    public ResumeResponse duplicateResume(Long resumeId, Long requesterUserId) {
        Resume source = getResumeEntityById(resumeId);
        requireOwner(source, requesterUserId);

        Resume duplicated = Resume.builder()
                .userId(source.getUserId())
                .title(source.getTitle() + " (Copy)")
            .name(source.getName())
            .email(source.getEmail())
            .phone(source.getPhone())
            .location(source.getLocation())
                .targetJobTitle(source.getTargetJobTitle())
                .templateId(source.getTemplateId())
                .atsScore(source.getAtsScore())
                .status(ResumeStatus.DRAFT)
                .language(source.getLanguage())
            .summary(source.getSummary())
            .skills(source.getSkills())
            .experience(source.getExperience())
            .education(source.getEducation())
            .projects(source.getProjects())
                .sectionsJson(source.getSectionsJson())
                .isPublic(Boolean.FALSE)
                .viewCount(0L)
                .build();

        Resume savedDuplicate = resumeRepository.save(duplicated);
        sectionServiceClient.copySections(source.getResumeId(), savedDuplicate.getResumeId(), requesterUserId);
        log.info("Duplicated resume {} into {} by user {}", resumeId, savedDuplicate.getResumeId(), requesterUserId);
        return mapToResponse(savedDuplicate);
    }

    @Override
    @Transactional
    public ResumeResponse updateAtsScore(Long resumeId, Double score, Long requesterUserId, boolean internalCall) {
        if (score == null || score < 0) {
            throw new InvalidInputException("ATS score must be a non-negative number");
        }
        if (!internalCall) {
            throw new InvalidInputException("ATS score updates are only allowed for internal service calls");
        }

        Resume resume = getResumeEntityById(resumeId);
        if (requesterUserId != null) {
            requireOwner(resume, requesterUserId);
        }
        resume.setAtsScore(score);

        Resume saved = resumeRepository.save(resume);
        log.info("Updated ATS score for resume {} by user {}", resumeId, requesterUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ResumeResponse generateAtsScoreWithAi(
            Long resumeId,
            Long requesterUserId,
            String resumeText,
            String jobDescription) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new InvalidInputException("Resume text must not be empty");
        }

        Resume resume = getResumeEntityById(resumeId);
        requireOwner(resume, requesterUserId);
        Double aiScore = geminiAtsClient.generateAtsScore(resumeText, jobDescription);
        resume.setAtsScore(aiScore);

        Resume saved = resumeRepository.save(resume);
        log.info("Generated ATS score via AI for resume {} by user {}", resumeId, requesterUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ResumeResponse publishResume(Long resumeId, Long requesterUserId) {
        Resume resume = getResumeEntityById(resumeId);
        requireOwner(resume, requesterUserId);
        resume.setIsPublic(Boolean.TRUE);
        Resume saved = resumeRepository.save(resume);
        log.info("Published resume {} by user {}", resumeId, requesterUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ResumeResponse unpublishResume(Long resumeId, Long requesterUserId) {
        Resume resume = getResumeEntityById(resumeId);
        requireOwner(resume, requesterUserId);
        resume.setIsPublic(Boolean.FALSE);
        Resume saved = resumeRepository.save(resume);
        log.info("Unpublished resume {} by user {}", resumeId, requesterUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long resumeId) {
        Resume resume = getResumeEntityById(resumeId);
        if (!Boolean.TRUE.equals(resume.getIsPublic())) {
            throw new InvalidInputException("View count can be incremented only for public resumes");
        }
        resume.setViewCount(resume.getViewCount() + 1);
        resumeRepository.save(resume);
        log.info("Incremented view count for public resume {}", resumeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getPublicResumes() {
        return resumeRepository.findByIsPublic(Boolean.TRUE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesByTemplate(Long templateId) {
        return resumeRepository.findByTemplateId(templateId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Resume getResumeEntityById(Long resumeId) {
        return resumeRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found with id: " + resumeId));
    }

    private ResumeResponse mapToResponse(Resume resume) {
        return ResumeResponse.builder()
                .resumeId(resume.getResumeId())
                .userId(resume.getUserId())
            .name(resume.getName())
                .title(resume.getTitle())
            .email(resume.getEmail())
            .phone(resume.getPhone())
            .location(resume.getLocation())
                .targetJobTitle(resume.getTargetJobTitle())
                .templateId(resume.getTemplateId())
                .language(resume.getLanguage())
            .summary(resume.getSummary())
            .skills(resume.getSkills())
            .experience(resume.getExperience())
            .education(resume.getEducation())
            .projects(resume.getProjects())
                .sectionsJson(resume.getSectionsJson())
                .atsScore(resume.getAtsScore())
                .status(resume.getStatus())
                .isPublic(resume.getIsPublic())
                .viewCount(resume.getViewCount())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    private void requireAuthenticatedUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidInputException("Authenticated user context is required");
        }
    }

    private void requireOwner(Resume resume, Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        if (!resume.getUserId().equals(requesterUserId)) {
            throw new InvalidInputException("Access denied for this resume");
        }
    }

    private void enforceFreePlanLimit(Long userId, String authHeader, String userPlan) {
        if (userId == null) {
            return;
        }
        // Check X-User-Plan header first (set by gateway), then fall back to JWT parsing
        if ("PREMIUM".equalsIgnoreCase(userPlan)) {
            log.debug("User {} is PREMIUM via X-User-Plan header, skipping free limit check", userId);
            return;
        }
        if (isPremiumUser(authHeader)) {
            log.debug("User {} is PREMIUM via JWT claim, skipping free limit check", userId);
            return;
        }
        long existing = resumeRepository.countByUserId(userId);
        if (existing >= 3) {
            throw new InvalidInputException(
                    "Free plan allows up to 3 resumes. Upgrade to Premium for unlimited resumes.");
        }
    }

    private boolean isPremiumUser(String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(payload);
            String plan = node.path("subscriptionPlan").asText("");
            return "PREMIUM".equalsIgnoreCase(plan);
        } catch (Exception ex) {
            log.warn("Unable to parse subscription plan from JWT: {}", ex.getMessage());
            return false;
        }
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
