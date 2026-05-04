package com.resumeai.jobmatch.controller;

import com.resumeai.jobmatch.dto.BookmarkRequest;
import com.resumeai.jobmatch.dto.AnalysisResponse;
import com.resumeai.jobmatch.dto.JobResponse;
import com.resumeai.jobmatch.dto.MatchRequest;
import com.resumeai.jobmatch.dto.MatchResponse;
import com.resumeai.jobmatch.service.JobMatchService;
import com.resumeai.jobmatch.service.AdzunaService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/jobmatch", "/jobs", "/job-matches"})
@RequiredArgsConstructor
@Slf4j
public class JobMatchController {

    private final JobMatchService jobMatchService;
    private final AdzunaService adzunaService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String location) {
        
        // STEP 2: COMPREHENSIVE NULL CHECKS AT CONTROLLER LEVEL
        System.out.println("STEP 1: File received");
        log.info("=== /analyze endpoint called ===");
        log.info("resumeId: {}, userId: {}, file: {}", resumeId, userId, file != null ? file.getOriginalFilename() : "null");
        
        if ((file == null || file.isEmpty()) && resumeId == null) {
            log.warn("VALIDATION ERROR: Both file and resumeId are null");
                return ResponseEntity.badRequest().body(java.util.Map.of(
                    "status", "failed",
                    "message", "Resume required - either upload a PDF or select an existing resume"));
        }
        
        if (userId == null || userId <= 0) {
            log.warn("VALIDATION ERROR: userId is null or invalid: {}", userId);
                return ResponseEntity.badRequest().body(java.util.Map.of(
                    "status", "failed",
                    "message", "Valid userId is required"));
        }
        
        try {
            log.info("Starting analysis for userId: {}", userId);
            AnalysisResponse analysis = jobMatchService.analyzeAndMatchDetailed(file, resumeId, userId, jobTitle, location);
            log.info("Analysis completed successfully. Found {} matches", analysis.getTotalMatches());
                return ResponseEntity.ok(java.util.Map.of(
                    "status", "success",
                    "data", java.util.Map.of(
                        "jobs", analysis.getJobs(),
                        "extractedData", analysis.getExtractedData(),
                        "matches", analysis.getMatches())));
        } catch (Exception e) {
            log.error("CRITICAL ERROR in /analyze endpoint", e);
            e.printStackTrace();
                    return ResponseEntity.status(200).body(java.util.Map.of(
                        "status", "failed",
                        "message", e.getMessage() != null ? e.getMessage() : "AI unavailable"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query) {
        log.info("QUERY: {}", query);
        return ResponseEntity.ok(adzunaService.fetchJobs(query));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<MatchResponse>> getRankedJobs(@RequestParam Long userId) {
        return ResponseEntity.ok(jobMatchService.getRankedJobs(userId));
    }

    @GetMapping("/saved-jobs")
    public ResponseEntity<List<JobResponse>> getSavedJobs() {
        return ResponseEntity.ok(jobMatchService.fetchSavedJobs());
    }

    @GetMapping("/top")
    public ResponseEntity<?> getTopMatches(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        // STEP 5: Null check for userId
        if (userId == null || userId <= 0) {
            log.warn("STEP 5: /top endpoint called with invalid userId: {}", userId);
            return ResponseEntity.badRequest().body("ERROR: Valid userId is required");
        }
        
        if (limit <= 0) {
            limit = 10;
        }
        
        try {
            log.info("STEP 10: /top endpoint called - userId: {}, limit: {}", userId, limit);
            List<MatchResponse> matches = jobMatchService.getTopMatches(userId, limit);
            
            // STEP 5: Ensure matches is never null
            if (matches == null) {
                log.warn("STEP 5: Service returned null matches");
                matches = new ArrayList<>();
            }
            
            log.info("STEP 10: /top endpoint returning {} matches", matches.size());
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            log.error("STEP 5: Error in /top endpoint", e);
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/bookmark")
    public ResponseEntity<MatchResponse> bookmark(@Valid @RequestBody BookmarkRequest request) {
        return ResponseEntity.ok(jobMatchService.updateBookmark(request.getMatchId(), request.getBookmarked()));
    }

    // Backward-compatible endpoints
    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(jobMatchService.fetchJobs(keyword));
    }

    @PostMapping("/match")
    public ResponseEntity<List<MatchResponse>> matchResumeWithJobs(@Valid @RequestBody MatchRequest request) {
        return ResponseEntity.ok(jobMatchService.matchResumeWithJobs(
                request.getUserId(),
                request.getResumeId(),
                request.getJobTitle(),
                request.getJobDescription()));
    }

    @GetMapping("/matches/{userId}")
    public ResponseEntity<List<MatchResponse>> getMatchesForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(jobMatchService.getMatchesForUser(userId));
    }

    @GetMapping("/matches/resume/{resumeId}")
    public ResponseEntity<List<MatchResponse>> getMatchesForResume(@PathVariable Long resumeId) {
        return ResponseEntity.ok(jobMatchService.getMatchesForResume(resumeId));
    }

    @GetMapping("/matches/id/{matchId}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable UUID matchId) {
        return ResponseEntity.ok(jobMatchService.getMatchById(matchId));
    }

    @PutMapping("/matches/{matchId}/bookmark")
    public ResponseEntity<MatchResponse> toggleBookmark(@PathVariable UUID matchId) {
        return ResponseEntity.ok(jobMatchService.toggleBookmark(matchId));
    }

    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<Void> deleteMatch(@PathVariable UUID matchId) {
        jobMatchService.deleteMatch(matchId);
        return ResponseEntity.noContent().build();
    }
}
