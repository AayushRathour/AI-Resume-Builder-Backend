package com.resumeai.jobmatch.dto;

import com.resumeai.jobmatch.entity.JobSource;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {

    private Long jobId;
    private String title;
    private String company;
    private String location;
    private String description;
    private String requiredSkills;
    private JobSource source;
    private LocalDateTime createdAt;
}
