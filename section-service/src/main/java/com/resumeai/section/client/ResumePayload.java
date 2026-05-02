package com.resumeai.section.client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumePayload {

    private Long resumeId;
    private Long userId;
    private Boolean isPublic;
}
