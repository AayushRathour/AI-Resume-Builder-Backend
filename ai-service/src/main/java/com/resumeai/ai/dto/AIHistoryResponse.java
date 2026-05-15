package com.resumeai.ai.dto;

import java.time.LocalDateTime;

import com.resumeai.ai.entity.RequestStatus;
import com.resumeai.ai.entity.RequestType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for aihistory operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIHistoryResponse {
    private String requestId;
    private Long userId;
    private Long resumeId;
    private RequestType requestType;
    private String inputPrompt;
    private String aiResponse;
    private String model;
    private RequestStatus status;
    private Integer tokensUsed;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}



