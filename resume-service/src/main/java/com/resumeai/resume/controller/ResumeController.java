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
import com.resumeai.resume.dto.ResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.service.ResumeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({ "/resumes", "/resume" })
@RequiredArgsConstructor
@Validated
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeService resumeService;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.createResume(userId, request, authHeader, userPlan));
    }

    @PostMapping("/save")
    public ResponseEntity<ResumeResponse> saveResume(
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Plan", required = false) String userPlan,
            @RequestParam(value = "userId", required = false) Long fallbackUserId,
            @Valid @RequestBody ResumeRequest request) {
        return createResume(authenticatedUserId, authHeader, userPlan, fallbackUserId, request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(resumeService.getResumeById(resumeId, requesterUserId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ResumeResponse>> getResumesByUser(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(resumeService.getResumesByUser(userId, requesterUserId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @Valid @RequestBody ResumeRequest request) {
        log.info("Update resume request resumeId={}, userId={}, templateId={}", resumeId, requesterUserId, request.getTemplateId());
        log.info("Update resume sectionsJson length: {}", request.getSectionsJson() == null ? 0 : request.getSectionsJson().length());
        return ResponseEntity.ok(resumeService.updateResume(resumeId, requesterUserId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        resumeService.deleteResume(resumeId, requesterUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ResumeResponse> duplicateResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.duplicateResume(resumeId, requesterUserId));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ResumeResponse> publishResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(resumeService.publishResume(resumeId, requesterUserId));
    }

    @PutMapping("/{id}/unpublish")
    public ResponseEntity<ResumeResponse> unpublishResume(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(resumeService.unpublishResume(resumeId, requesterUserId));
    }

    @PutMapping({ "/{id}/atsScore", "/{id}/ats-score" })
    public ResponseEntity<ResumeResponse> updateAtsScore(
            @PathVariable("id") Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @RequestHeader(value = "X-Internal-Call", required = false, defaultValue = "false") boolean internalCall,
            @RequestParam Double score) {
        return ResponseEntity.ok(resumeService.updateAtsScore(resumeId, score, requesterUserId, internalCall));
    }

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

    @PutMapping({ "/{id}/view", "/{id}/increment-view" })
    public ResponseEntity<Void> incrementViewCount(@PathVariable("id") Long resumeId) {
        resumeService.incrementViewCount(resumeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public")
    public ResponseEntity<List<ResumeResponse>> getPublicResumes() {
        return ResponseEntity.ok(resumeService.getPublicResumes());
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<ResumeResponse>> getResumesByTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(resumeService.getResumesByTemplate(templateId));
    }
}
