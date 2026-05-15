package com.resumeai.template.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent entity used by this service domain. */

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    @Column(nullable = false)
    private String name;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private TemplateCategory category = TemplateCategory.PROFESSIONAL;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "LONGTEXT")
    private String htmlContent;

    @Column(columnDefinition = "LONGTEXT")
    private String cssContent;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String fieldsJson = "[{\"key\":\"name\",\"label\":\"Full Name\"},{\"key\":\"title\",\"label\":\"Job Title\"},{\"key\":\"email\",\"label\":\"Email\"},{\"key\":\"phone\",\"label\":\"Phone Number\"},{\"key\":\"location\",\"label\":\"Location\"},{\"key\":\"linkedin\",\"label\":\"LinkedIn Profile\"},{\"key\":\"github\",\"label\":\"GitHub Profile\"},{\"key\":\"summary\",\"label\":\"Professional Summary\"},{\"key\":\"skills\",\"label\":\"Skills\"},{\"key\":\"experience\",\"label\":\"Work Experience\"},{\"key\":\"education\",\"label\":\"Education\"}]";

    private String previewImageUrl;

    @Column(nullable = false)
    private boolean isPremium;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.category == null) {
            this.category = TemplateCategory.PROFESSIONAL;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

