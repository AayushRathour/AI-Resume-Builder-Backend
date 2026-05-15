package com.resumeai.section.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.resumeai.section.entity.Section;
import com.resumeai.section.entity.SectionType;

/** Repository for persistence and query operations in this domain. */

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByResumeId(Long resumeId);

    List<Section> findByResumeIdAndSectionType(Long resumeId, SectionType sectionType);

    Optional<Section> findBySectionId(Long sectionId);

    List<Section> findByResumeIdOrderByDisplayOrderAsc(Long resumeId);

    List<Section> findByAiGenerated(Boolean aiGenerated);

    long countByResumeId(Long resumeId);

    void deleteByResumeId(Long resumeId);

    void deleteBySectionId(Long sectionId);
}

