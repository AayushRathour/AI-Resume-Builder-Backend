package com.resumeai.jobmatch.controller;

import com.resumeai.jobmatch.dto.JobResponse;
import com.resumeai.jobmatch.dto.MatchRequest;
import com.resumeai.jobmatch.dto.MatchResponse;
import com.resumeai.jobmatch.service.JobMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * All endpoints are under /jobs so they match the gateway predicate
 * /api/jobs/**
 * (with server.servlet.context-path=/api set in application.properties).
 */
@RestController
@RequestMapping({ "/jobs", "/jobmatch", "/job-matches" })
@RequiredArgsConstructor
@Slf4j
public class JobMatchController {

    private final JobMatchService jobMatchService;

    /**
     * GET /api/jobs?keyword=developer
     * Returns all jobs, optionally filtered by title keyword.
     */
    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(
            @RequestParam(required = false) String keyword) {
        log.info("GET /jobs?keyword={}", keyword);
        return ResponseEntity.ok(jobMatchService.fetchJobs(keyword));
    }

    /**
     * POST /api/jobs/match
     * Body: { "userId": 1, "resumeId": 1 }
     * Matches the resume against all jobs and persists scores.
     */
    @PostMapping("/match")
    public ResponseEntity<List<MatchResponse>> matchResumeWithJobs(
            @Valid @RequestBody MatchRequest request) {
        log.info("POST /jobs/match userId={}, resumeId={}", request.getUserId(), request.getResumeId());
        return ResponseEntity.ok(jobMatchService.matchResumeWithJobs(
                request.getUserId(), request.getResumeId(), request.getJobTitle(), request.getJobDescription()));
    }

    /**
     * GET /api/jobs/matches/{userId}
     * Returns all previously computed matches for a user.
     */
    @GetMapping("/matches/{userId}")
    public ResponseEntity<List<MatchResponse>> getMatchesForUser(
            @PathVariable Long userId) {
        log.info("GET /jobs/matches/{}", userId);
        return ResponseEntity.ok(jobMatchService.getMatchesForUser(userId));
    }

    @GetMapping("/matches/resume/{resumeId}")
    public ResponseEntity<List<MatchResponse>> getMatchesForResume(@PathVariable Long resumeId) {
        log.info("GET /jobs/matches/resume/{}", resumeId);
        return ResponseEntity.ok(jobMatchService.getMatchesForResume(resumeId));
    }

    @GetMapping("/matches/id/{matchId}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable UUID matchId) {
        log.info("GET /jobs/matches/id/{}", matchId);
        return ResponseEntity.ok(jobMatchService.getMatchById(matchId));
    }

    @PutMapping("/matches/{matchId}/bookmark")
    public ResponseEntity<MatchResponse> toggleBookmark(@PathVariable UUID matchId) {
        log.info("PUT /jobs/matches/{}/bookmark", matchId);
        return ResponseEntity.ok(jobMatchService.toggleBookmark(matchId));
    }

    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<Void> deleteMatch(@PathVariable UUID matchId) {
        log.info("DELETE /jobs/matches/{}", matchId);
        jobMatchService.deleteMatch(matchId);
        return ResponseEntity.noContent().build();
    }
}
