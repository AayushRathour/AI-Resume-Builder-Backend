package com.resumeai.jobmatch.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import com.resumeai.jobmatch.entity.JobSource;

/** Response payload for job matching operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponse {

    private UUID matchId;
    private Long userId;
    private Long resumeId;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String location;
    private String jobDescription;
    private String applyUrl;
    private String source;
    private Double matchScore;
    private String missingSkills;
    private String recommendation;
    private boolean isBookmarked;
    private LocalDateTime createdAt;
}



