package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.client.ResumeClient;
import com.resumeai.jobmatch.client.SectionClient;
import com.resumeai.jobmatch.dto.JobResponse;
import com.resumeai.jobmatch.dto.MatchResponse;
import com.resumeai.jobmatch.dto.NotificationEvent;
import com.resumeai.jobmatch.dto.ResumeDTO;
import com.resumeai.jobmatch.dto.SectionDTO;
import com.resumeai.jobmatch.entity.Job;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import com.resumeai.jobmatch.repository.JobRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchService {

    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final ResumeClient resumeClient;
    private final SectionClient sectionClient;
    private final RabbitTemplate rabbitTemplate;

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

    @Transactional
    public List<MatchResponse> matchResumeWithJobs(Long userId, Long resumeId, String customJobTitle, String customJobDesc) {
        log.info("Starting job match for userId={}, resumeId={}", userId, resumeId);

        Set<String> resumeSkills = extractSkillsFromResume(resumeId);
        List<Job> jobsToMatch = new ArrayList<>();

        if (customJobTitle != null && !customJobTitle.isBlank() && customJobDesc != null && !customJobDesc.isBlank()) {
            Job customJob = jobRepository.save(Job.builder()
                    .title(customJobTitle)
                    .company("Custom Manual Entry")
                    .description(customJobDesc)
                    .requiredSkills(customJobDesc)
                    .source(JobSource.MANUAL)
                    .build());
            jobsToMatch.add(customJob);
        } else {
            jobsToMatch.addAll(jobRepository.findAll());
        }

        if (jobsToMatch.isEmpty()) {
            log.warn("No jobs found in database; returning empty match list");
            return Collections.emptyList();
        }

        // If we are doing a bulk match, we might clear previous matches. But for custom, maybe not?
        // Let's just always delete for simplicity, or we can keep history.
        // Actually, deleting old matches makes sense to not spam the DB with stale data.
        // But for custom matches, maybe we don't delete everything.
        if (customJobTitle == null || customJobTitle.isBlank()) {
            jobMatchRepository.deleteByUserIdAndResumeId(userId, resumeId);
        }

        List<MatchResponse> results = new ArrayList<>();
        for (Job job : jobsToMatch) {
            Set<String> jobSkills = parseSkills(job.getRequiredSkills());
            double score = calculateMatchScore(resumeSkills, jobSkills);
            String missing = findMissingSkills(resumeSkills, jobSkills);
            String recommendation = generateRecommendation(score, missing, job.getTitle());

            JobMatch saved = jobMatchRepository.save(JobMatch.builder()
                    .userId(userId)
                    .resumeId(resumeId)
                    .jobId(job.getJobId())
                    .matchScore(score)
                    .missingSkills(missing)
                    .recommendation(recommendation)
                    .isBookmarked(false)
                    .build());

            results.add(toMatchResponse(saved, job));
        }

        results.sort(Comparator.comparingDouble(MatchResponse::getMatchScore).reversed());
        publishMatchNotification(userId, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesForUser(Long userId) {
        return jobMatchRepository.findByUserId(userId).stream()
                .map(match -> toMatchResponse(match, jobRepository.findById(match.getJobId()).orElse(null)))
                .sorted(Comparator.comparing(MatchResponse::getCreatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesForResume(Long resumeId) {
        return jobMatchRepository.findByResumeId(resumeId).stream()
                .map(match -> toMatchResponse(match, jobRepository.findById(match.getJobId()).orElse(null)))
                .sorted(Comparator.comparing(MatchResponse::getCreatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatchById(UUID matchId) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + matchId));
        Job job = jobRepository.findById(match.getJobId()).orElse(null);
        return toMatchResponse(match, job);
    }

    @Transactional
    public MatchResponse toggleBookmark(UUID matchId) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + matchId));
        match.setBookmarked(!match.isBookmarked());
        JobMatch saved = jobMatchRepository.save(match);
        Job job = jobRepository.findById(saved.getJobId()).orElse(null);
        return toMatchResponse(saved, job);
    }

    @Transactional
    public void deleteMatch(UUID matchId) {
        if (!jobMatchRepository.existsById(matchId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + matchId);
        }
        jobMatchRepository.deleteById(matchId);
    }

    public double calculateMatchScore(Set<String> resumeSkills, Set<String> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) {
            return 0.0;
        }

        Set<String> normalizedResume = normalizeSkills(resumeSkills);
        Set<String> normalizedJob = normalizeSkills(jobSkills);

        long matched = normalizedJob.stream()
                .filter(normalizedResume::contains)
                .count();

        double score = ((double) matched / normalizedJob.size()) * 100.0;
        return Math.round(score * 100.0) / 100.0;
    }

    private Set<String> extractSkillsFromResume(Long resumeId) {
        Set<String> skills = new HashSet<>();

        try {
            List<SectionDTO> sections = sectionClient.getSectionsByResumeId(resumeId);
            if (sections != null) {
                for (SectionDTO section : sections) {
                    if ("SKILLS".equalsIgnoreCase(section.getSectionType()) && section.getContent() != null) {
                        skills.addAll(parseSkills(section.getContent()));
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not fetch sections for resumeId={}: {}", resumeId, ex.getMessage());
        }

        if (skills.isEmpty()) {
            try {
                ResumeDTO resume = resumeClient.getResumeById(resumeId);
                if (resume != null && resume.getTargetJobTitle() != null) {
                    skills.addAll(parseSkills(resume.getTargetJobTitle()));
                }
            } catch (Exception ex) {
                log.warn("Could not fetch resume for resumeId={}: {}", resumeId, ex.getMessage());
            }
        }

        return skills;
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

    private Set<String> normalizeSkills(Set<String> skills) {
        return skills.stream()
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());
    }

    private String findMissingSkills(Set<String> resumeSkills, Set<String> jobSkills) {
        Set<String> normalizedResume = normalizeSkills(resumeSkills);
        return jobSkills.stream()
                .filter(skill -> !normalizedResume.contains(skill.toLowerCase(Locale.ROOT).trim()))
                .collect(Collectors.joining(", "));
    }

    private String generateRecommendation(double score, String missingSkills, String jobTitle) {
        if (score >= 80) {
            return String.format("Excellent match for '%s'.", jobTitle);
        }
        if (score >= 60) {
            return String.format("Good match for '%s'. Improve: %s.", jobTitle,
                    missingSkills.isEmpty() ? "soft skills and domain experience" : missingSkills);
        }
        if (score >= 40) {
            return String.format("Moderate match for '%s'. Focus on: %s.", jobTitle,
                    missingSkills.isEmpty() ? "core technical skills" : missingSkills);
        }
        return String.format("Low match for '%s'. Recommended skills: %s.", jobTitle,
                missingSkills.isEmpty() ? "review role requirements carefully" : missingSkills);
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

    private MatchResponse toMatchResponse(JobMatch match, Job job) {
        return MatchResponse.builder()
                .matchId(match.getMatchId())
                .userId(match.getUserId())
                .resumeId(match.getResumeId())
                .jobId(match.getJobId())
                .jobTitle(job != null ? job.getTitle() : "Unknown")
                .company(job != null ? job.getCompany() : "Unknown")
                .location(job != null ? job.getLocation() : null)
                .source(job != null ? job.getSource() : null)
                .matchScore(match.getMatchScore())
                .missingSkills(match.getMissingSkills())
                .recommendation(match.getRecommendation())
                .isBookmarked(match.isBookmarked())
                .createdAt(match.getCreatedAt())
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
}
