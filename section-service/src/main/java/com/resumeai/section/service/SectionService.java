package com.resumeai.section.service;

import java.util.List;

import com.resumeai.section.dto.SectionReorderItemRequest;
import com.resumeai.section.dto.SectionRequest;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.SectionUpdateItemRequest;
import com.resumeai.section.entity.SectionType;

/** Defines the service contract for core business operations. */

public interface SectionService {

    SectionResponse addSection(SectionRequest request, Long requesterUserId);

    List<SectionResponse> getSectionsByResume(Long resumeId, Long requesterUserId);

    SectionResponse getSectionById(Long sectionId, Long requesterUserId);

    SectionResponse updateSection(Long sectionId, SectionRequest request, Long requesterUserId);

    void deleteSection(Long sectionId, Long requesterUserId);

    List<SectionResponse> reorderSections(Long resumeId, List<SectionReorderItemRequest> orderRequests, Long requesterUserId);

    SectionResponse toggleVisibility(Long sectionId, Boolean isVisible, Long requesterUserId);

    void deleteAllSections(Long resumeId, Long requesterUserId);

    List<SectionResponse> getSectionsByType(Long resumeId, SectionType sectionType, Long requesterUserId);

    List<SectionResponse> bulkUpdateSections(Long resumeId, List<SectionUpdateItemRequest> updates, Long requesterUserId);
}

