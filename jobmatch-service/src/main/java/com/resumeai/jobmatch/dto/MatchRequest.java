package com.resumeai.jobmatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "resumeId is required")
    private Long resumeId;

    private String jobTitle;

    private String jobDescription;
}
