package com.resumeai.section.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.section.dto.BulkSectionUpdateRequest;
import com.resumeai.section.dto.SectionReorderItemRequest;
import com.resumeai.section.dto.SectionRequest;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.SectionVisibilityRequest;
import com.resumeai.section.entity.SectionType;
import com.resumeai.section.service.SectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Exposes REST endpoints for section workflows. */

@RestController
@RequestMapping("/sections")
@RequiredArgsConstructor
@Validated
public class SectionController {

    private final SectionService sectionService;

    @PostMapping
    public ResponseEntity<SectionResponse> addSection(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.addSection(request, requesterUserId));
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> getSectionById(
            @PathVariable Long sectionId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(sectionService.getSectionById(sectionId, requesterUserId));
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<List<SectionResponse>> getSectionsByResume(
            @PathVariable Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(sectionService.getSectionsByResume(resumeId, requesterUserId));
    }

    @GetMapping("/resume/{resumeId}/type/{sectionType}")
    public ResponseEntity<List<SectionResponse>> getSectionsByType(
            @PathVariable Long resumeId,
            @PathVariable SectionType sectionType,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        return ResponseEntity.ok(sectionService.getSectionsByType(resumeId, sectionType, requesterUserId));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> updateSection(
            @PathVariable Long sectionId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.ok(sectionService.updateSection(sectionId, request, requesterUserId));
    }

    @PutMapping("/reorder/{resumeId}")
    public ResponseEntity<List<SectionResponse>> reorderSections(
            @PathVariable Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @Valid @RequestBody List<SectionReorderItemRequest> orderRequests) {
        return ResponseEntity.ok(sectionService.reorderSections(resumeId, orderRequests, requesterUserId));
    }

    @PutMapping("/{sectionId}/visibility")
    public ResponseEntity<SectionResponse> toggleVisibility(
            @PathVariable Long sectionId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @Valid @RequestBody SectionVisibilityRequest request) {
        return ResponseEntity.ok(sectionService.toggleVisibility(sectionId, request.getIsVisible(), requesterUserId));
    }

    @PutMapping("/bulk/{resumeId}")
    public ResponseEntity<List<SectionResponse>> bulkUpdateSections(
            @PathVariable Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @Valid @RequestBody BulkSectionUpdateRequest request) {
        return ResponseEntity.ok(sectionService.bulkUpdateSections(resumeId, request.getSections(), requesterUserId));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long sectionId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        sectionService.deleteSection(sectionId, requesterUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/resume/{resumeId}")
    public ResponseEntity<Void> deleteAllSections(
            @PathVariable Long resumeId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId) {
        sectionService.deleteAllSections(resumeId, requesterUserId);
        return ResponseEntity.noContent().build();
    }
}



