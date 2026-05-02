package com.resumeai.resume.service;

import java.util.List;

import com.resumeai.resume.dto.ResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;

public interface ResumeService {

    ResumeResponse createResume(Long authenticatedUserId, ResumeRequest request, String authHeader, String userPlan);

    ResumeResponse getResumeById(Long resumeId, Long requesterUserId);

    List<ResumeResponse> getResumesByUser(Long userId, Long requesterUserId);

    ResumeResponse updateResume(Long resumeId, Long requesterUserId, ResumeRequest request);

    void deleteResume(Long resumeId, Long requesterUserId);

    ResumeResponse duplicateResume(Long resumeId, Long requesterUserId);

    ResumeResponse updateAtsScore(Long resumeId, Double score, Long requesterUserId, boolean internalCall);

    ResumeResponse generateAtsScoreWithAi(Long resumeId, Long requesterUserId, String resumeText,
            String jobDescription);

    ResumeResponse publishResume(Long resumeId, Long requesterUserId);

    ResumeResponse unpublishResume(Long resumeId, Long requesterUserId);

    void incrementViewCount(Long resumeId);

    List<ResumeResponse> getPublicResumes();

    List<ResumeResponse> getResumesByTemplate(Long templateId);
}
