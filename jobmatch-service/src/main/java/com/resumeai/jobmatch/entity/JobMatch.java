package com.resumeai.jobmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Persistent entity used by this service domain. */

@Entity
@Table(name = "job_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID matchId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long resumeId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private String jobTitle;

    private String company;

    private String location;

    @Column(length = 1024)
    private String applyUrl;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(nullable = false)
    private Double matchScore;

    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(length = 100)
    private String source;

    @Column(nullable = false)
    @Builder.Default
    private boolean isBookmarked = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

