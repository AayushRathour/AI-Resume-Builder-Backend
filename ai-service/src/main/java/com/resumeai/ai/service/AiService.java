package com.resumeai.ai.service;

import java.util.List;

import com.resumeai.ai.dto.AIHistoryResponse;
import com.resumeai.ai.dto.AIResponse;
import com.resumeai.ai.dto.ATSRequest;
import com.resumeai.ai.dto.ATSResponse;
import com.resumeai.ai.dto.BulletRequest;
import com.resumeai.ai.dto.CoverLetterRequest;
import com.resumeai.ai.dto.ImproveRequest;
import com.resumeai.ai.dto.QuotaResponse;
import com.resumeai.ai.dto.SkillRequest;
import com.resumeai.ai.dto.SummaryRequest;
import com.resumeai.ai.dto.TailorRequest;
import com.resumeai.ai.dto.TranslateRequest;

public interface AiService {

    AIResponse generateSummary(Long userId, Long resumeId, SummaryRequest request);

    AIResponse generateBulletPoints(Long userId, Long resumeId, BulletRequest request);

    AIResponse generateCoverLetter(Long userId, Long resumeId, CoverLetterRequest request);

    AIResponse improveSection(Long userId, Long resumeId, ImproveRequest request);

    ATSResponse checkAtsCompatibility(Long userId, Long resumeId, ATSRequest request);

    AIResponse suggestSkills(Long userId, Long resumeId, SkillRequest request);

    AIResponse tailorResumeForJob(Long userId, Long resumeId, TailorRequest request);

    AIResponse translateResume(Long userId, Long resumeId, TranslateRequest request);

    List<AIHistoryResponse> getAiHistory(Long userId);

    QuotaResponse getRemainingQuota(Long userId);
}
