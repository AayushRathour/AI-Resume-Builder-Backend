package com.resumeai.section.client;

import lombok.Getter;
import lombok.Setter;

/** Payload model used when reading resume visibility metadata from resume-service. */

@Getter
@Setter
public class ResumePayload {

    private Long resumeId;
    private Long userId;
    private Boolean isPublic;
}




