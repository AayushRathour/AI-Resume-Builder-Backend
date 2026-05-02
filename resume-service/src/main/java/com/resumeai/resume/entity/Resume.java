package com.resumeai.resume.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "resumes",
    indexes = {
        @Index(name = "idx_resumes_user_id", columnList = "user_id"),
        @Index(name = "idx_resumes_template_id", columnList = "template_id"),
        @Index(name = "idx_resumes_is_public", columnList = "is_public"),
        @Index(name = "idx_resumes_target_job_title", columnList = "target_job_title")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resumeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_job_title", nullable = false)
    private String targetJobTitle;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "ats_score", nullable = false)
    private Double atsScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResumeStatus status;

    @Column(nullable = false)
    private String language;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String sectionsJson;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
