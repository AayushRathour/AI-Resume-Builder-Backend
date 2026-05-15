package com.resumeai.resume.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.resume.dto.AtsScoreAiRequest;
import com.resumeai.resume.dto.AtsBackfillResponse;
import com.resumeai.resume.dto.ResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.service.ResumeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Resume API endpoints for CRUD, publishing, and ATS scoring.
 */
@RestController
@RequestMapping({ "/resumes", "/resume" })
@RequiredArgsConstructor
@Validated
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeService resumeService;

    /**
     * Creates a new resume and enforces plan limits.
     */
    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Plan", required = false) String userPlan,
            @RequestParam(value = "userId", required = false) Long fallbackUserId,
            @Valid @RequestBody ResumeRequest request) {
        Long userId = authenticatedUserId != null ? authenticatedUserId : fallbackUserId;
        log.info("Create resume request userId={}, title='{}', targetJobTitle='{}', templateId={}, language='{}', plan='{}'",
                userId,
                request.getTitle(),
                request.getTargetJobTitle(),
                request.getTemplateId(),
                request.getLanguage(),
                userPlan);
        log.info("Create resume sectionsJson length: {}", request.getSectionsJson() == null ? 0 : request.getSectionsJson().length());
        // Validate ownership and apply plan limits before creating.
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.createResume(userId, request, authHeader, userPlan));
    }

    /**
     * Returns resume by id, enforcing visibility rules.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(resumeService.getResumeById(resumeId, requesterUserId));
    }

    /**
     * Returns all resumes for the specified user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ResumeResponse>> getResumesByUser(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(resumeService.getResumesByUser(userId, requesterUserId));
    }

    /**
     * Updates an existing resume by id.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @Valid @RequestBody ResumeRequest request) {
        log.info("Update resume request resumeId={}, userId={}, templateId={}", resumeId, requesterUserId, request.getTemplateId());
        log.info("Update resume sectionsJson length: {}", request.getSectionsJson() == null ? 0 : request.getSectionsJson().length());
        return ResponseEntity.ok(resumeService.updateResume(resumeId, requesterUserId, request));
    }

    /**
     * Deletes a resume and its sections.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        resumeService.deleteResume(resumeId, requesterUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Duplicates a resume along with its sections.
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ResumeResponse> duplicateResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.duplicateResume(resumeId, requesterUserId));
    }

    /**
     * Publishes a resume for public visibility.
     */
    @PutMapping("/{id}/publish")
    public ResponseEntity<ResumeResponse> publishResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(resumeService.publishResume(resumeId, requesterUserId));
    }

    /**
     * Unpublishes a resume from public visibility.
     */
    @PutMapping("/{id}/unpublish")
    public ResponseEntity<ResumeResponse> unpublishResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(resumeService.unpublishResume(resumeId, requesterUserId));
    }

    /**
     * Updates ATS score via internal service calls only.
     */
    @PutMapping({ "/{id}/atsScore", "/{id}/ats-score" })
    public ResponseEntity<ResumeResponse> updateAtsScore(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @RequestHeader(value = "X-Internal-Call", required = false, defaultValue = "false") boolean internalCall,
            @RequestParam Double score) {
        return ResponseEntity.ok(resumeService.updateAtsScore(resumeId, score, requesterUserId, internalCall));
    }

    /**
     * Generates ATS score using AI with resume text and job description.
     */
    @PostMapping("/{id}/ats-score/ai")
    public ResponseEntity<ResumeResponse> updateAtsScoreWithAi(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @Valid @RequestBody AtsScoreAiRequest request) {
        return ResponseEntity.ok(
                resumeService.generateAtsScoreWithAi(
                        resumeId,
                        requesterUserId,
                        request.getResumeText(),
                        request.getJobDescription()));
    }

    /**
     * Recomputes missing/zero ATS scores for a user's resumes.
     */
    @PostMapping("/user/{userId}/ats-score/backfill")
    public ResponseEntity<AtsBackfillResponse> backfillAtsScores(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(resumeService.backfillAtsScores(userId, requesterUserId, limit));
    }

    /**
     * Increments view count for public resumes.
     */
    @PutMapping({ "/{id}/view", "/{id}/increment-view" })
    public ResponseEntity<Void> incrementViewCount(@PathVariable("id") Long resumeId) {
        resumeService.incrementViewCount(resumeId);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns publicly visible resumes.
     */
    @GetMapping("/public")
    public ResponseEntity<List<ResumeResponse>> getPublicResumes() {
        return ResponseEntity.ok(resumeService.getPublicResumes());
    }

    /**
     * Returns resumes that use a specific template.
     */
    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<ResumeResponse>> getResumesByTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(resumeService.getResumesByTemplate(templateId));
    }

    /**
     * Internal count endpoint for admin analytics.
     */
    @GetMapping("/internal/count")
    public ResponseEntity<Long> getInternalResumeCount(
            @RequestHeader(value = "X-Internal-Call", required = false, defaultValue = "false") boolean internalCall) {
        return ResponseEntity.ok(resumeService.countAllResumes(internalCall));
    }
}
