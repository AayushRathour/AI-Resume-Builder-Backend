package com.resumeai.section.entity;

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
    name = "sections",
    indexes = {
        @Index(name = "idx_sections_resume_id", columnList = "resume_id"),
        @Index(name = "idx_sections_section_type", columnList = "section_type"),
        @Index(name = "idx_sections_display_order", columnList = "display_order"),
        @Index(name = "idx_sections_ai_generated", columnList = "ai_generated")
    })

/** Persistent entity used by this service domain. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sectionId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, columnDefinition = "VARCHAR(64)")
    private SectionType sectionType;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

    @Column(name = "ai_generated", nullable = false)
    private Boolean aiGenerated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

