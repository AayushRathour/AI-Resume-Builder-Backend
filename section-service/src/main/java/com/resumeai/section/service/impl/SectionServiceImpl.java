package com.resumeai.section.service.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.section.client.ResumePayload;
import com.resumeai.section.client.ResumeServiceClient;
import com.resumeai.section.dto.SectionReorderItemRequest;
import com.resumeai.section.dto.SectionRequest;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.SectionUpdateItemRequest;
import com.resumeai.section.entity.Section;
import com.resumeai.section.entity.SectionType;
import com.resumeai.section.exception.InvalidInputException;
import com.resumeai.section.exception.SectionNotFoundException;
import com.resumeai.section.repository.SectionRepository;
import com.resumeai.section.service.SectionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private static final Logger log = LoggerFactory.getLogger(SectionServiceImpl.class);

    private final SectionRepository sectionRepository;
    private final ResumeServiceClient resumeServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public SectionResponse addSection(SectionRequest request, Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        validateResumeOwnership(request.getResumeId(), requesterUserId);

        Section section = Section.builder()
                .resumeId(request.getResumeId())
                .sectionType(request.getSectionType())
                .title(request.getTitle())
                .content(normalizeStructuredContent(request.getContent()))
                .displayOrder(request.getDisplayOrder())
                .isVisible(request.getIsVisible())
                .aiGenerated(request.getAiGenerated())
                .build();

        Section saved = sectionRepository.save(section);
        log.info("Added section {} for resume {} by user {}", saved.getSectionId(), saved.getResumeId(), requesterUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByResume(Long resumeId, Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        validateResumeOwnership(resumeId, requesterUserId);

        return sectionRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SectionResponse getSectionById(Long sectionId, Long requesterUserId) {
        Section section = getSectionEntityById(sectionId);
        validateResumeOwnership(section.getResumeId(), requesterUserId);
        return mapToResponse(section);
    }

    @Override
    @Transactional
    public SectionResponse updateSection(Long sectionId, SectionRequest request, Long requesterUserId) {
        Section section = getSectionEntityById(sectionId);
        validateResumeOwnership(section.getResumeId(), requesterUserId);
        if (!section.getResumeId().equals(request.getResumeId())) {
            throw new InvalidInputException("Resume id in payload does not match section owner resume");
        }

        section.setSectionType(request.getSectionType());
        section.setTitle(request.getTitle());
        section.setContent(normalizeStructuredContent(request.getContent()));
        section.setDisplayOrder(request.getDisplayOrder());
        section.setIsVisible(request.getIsVisible());
        section.setAiGenerated(request.getAiGenerated());

        Section saved = sectionRepository.save(section);
        log.info("Updated section {} by user {}", sectionId, requesterUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId, Long requesterUserId) {
        Section section = getSectionEntityById(sectionId);
        validateResumeOwnership(section.getResumeId(), requesterUserId);
        sectionRepository.delete(section);
        log.info("Deleted section {} by user {}", sectionId, requesterUserId);
    }

    @Override
    @Transactional
    public List<SectionResponse> reorderSections(
            Long resumeId,
            List<SectionReorderItemRequest> orderRequests,
            Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        validateResumeOwnership(resumeId, requesterUserId);

        List<Section> sections = sectionRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId);

        if (sections.isEmpty()) {
            return List.of();
        }

        if (sections.size() != orderRequests.size()) {
            throw new InvalidInputException("Reorder list must include all sections for this resume");
        }

        Map<Long, Section> sectionMap = new HashMap<>();
        for (Section section : sections) {
            sectionMap.put(section.getSectionId(), section);
        }

        List<SectionReorderItemRequest> sortedRequests = orderRequests.stream()
                .sorted(Comparator.comparingInt(SectionReorderItemRequest::getDisplayOrder))
                .toList();

        for (SectionReorderItemRequest request : sortedRequests) {
            if (!sectionMap.containsKey(request.getSectionId())) {
                throw new InvalidInputException(
                        "Section id " + request.getSectionId() + " does not belong to resume id " + resumeId);
            }
        }

        for (int index = 0; index < sortedRequests.size(); index++) {
            Section section = sectionMap.get(sortedRequests.get(index).getSectionId());
            section.setDisplayOrder(index + 1);
        }

        List<SectionResponse> reordered = sectionRepository.saveAll(sections)
                .stream()
                .sorted(Comparator.comparingInt(Section::getDisplayOrder))
                .map(this::mapToResponse)
                .toList();

        log.info("Reordered {} sections for resume {} by user {}", reordered.size(), resumeId, requesterUserId);
        return reordered;
    }

    @Override
    @Transactional
    public SectionResponse toggleVisibility(Long sectionId, Boolean isVisible, Long requesterUserId) {
        Section section = getSectionEntityById(sectionId);
        validateResumeOwnership(section.getResumeId(), requesterUserId);
        section.setIsVisible(isVisible);

        Section saved = sectionRepository.save(section);
        log.info("Changed visibility for section {} to {} by user {}", sectionId, isVisible, requesterUserId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAllSections(Long resumeId, Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        validateResumeOwnership(resumeId, requesterUserId);
        sectionRepository.deleteByResumeId(resumeId);
        log.info("Deleted all sections for resume {} by user {}", resumeId, requesterUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByType(Long resumeId, SectionType sectionType, Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        validateResumeOwnership(resumeId, requesterUserId);
        return sectionRepository.findByResumeIdAndSectionType(resumeId, sectionType)
                .stream()
                .sorted(Comparator.comparingInt(Section::getDisplayOrder))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<SectionResponse> bulkUpdateSections(
            Long resumeId,
            List<SectionUpdateItemRequest> updates,
            Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        validateResumeOwnership(resumeId, requesterUserId);

        List<Section> existingSections = sectionRepository.findByResumeId(resumeId);
        Map<Long, Section> byId = new HashMap<>();
        for (Section section : existingSections) {
            byId.put(section.getSectionId(), section);
        }

        for (SectionUpdateItemRequest update : updates) {
            Section section = byId.get(update.getSectionId());
            if (section == null) {
                throw new SectionNotFoundException("Section not found with id: " + update.getSectionId());
            }

            section.setSectionType(update.getSectionType());
            section.setTitle(update.getTitle());
            section.setContent(normalizeStructuredContent(update.getContent()));
            section.setDisplayOrder(update.getDisplayOrder());
            section.setIsVisible(update.getIsVisible());
            section.setAiGenerated(update.getAiGenerated());
        }

        List<SectionResponse> saved = sectionRepository.saveAll(existingSections)
                .stream()
                .sorted(Comparator.comparingInt(Section::getDisplayOrder))
                .map(this::mapToResponse)
                .toList();

        log.info("Bulk updated {} sections for resume {} by user {}", updates.size(), resumeId, requesterUserId);
        return saved;
    }

    private Section getSectionEntityById(Long sectionId) {
        return sectionRepository.findBySectionId(sectionId)
                .orElseThrow(() -> new SectionNotFoundException("Section not found with id: " + sectionId));
    }

    private SectionResponse mapToResponse(Section section) {
        return SectionResponse.builder()
                .sectionId(section.getSectionId())
                .resumeId(section.getResumeId())
                .sectionType(section.getSectionType())
                .title(section.getTitle())
                .content(section.getContent())
                .displayOrder(section.getDisplayOrder())
                .isVisible(section.getIsVisible())
                .aiGenerated(section.getAiGenerated())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }

    private void validateResumeOwnership(Long resumeId, Long requesterUserId) {
        requireAuthenticatedUser(requesterUserId);
        ResumePayload resume = resumeServiceClient.getResumeById(resumeId, requesterUserId);
        if (resume == null || resume.getUserId() == null || !resume.getUserId().equals(requesterUserId)) {
            throw new InvalidInputException("Access denied for resume sections");
        }
    }

    private void requireAuthenticatedUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidInputException("Authenticated user context is required");
        }
    }

    private String normalizeStructuredContent(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "{}";
        }

        try {
            objectMapper.readTree(rawContent);
            return rawContent;
        } catch (JsonProcessingException ex) {
            try {
                return objectMapper.writeValueAsString(Map.of("text", rawContent));
            } catch (JsonProcessingException jsonException) {
                throw new InvalidInputException("Unable to serialize section content");
            }
        }
    }
}
