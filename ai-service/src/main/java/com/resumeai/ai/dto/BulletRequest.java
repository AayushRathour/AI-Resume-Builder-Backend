package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for bullet operations. */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulletRequest {
    private String jobTitle;
    private String companyName;
    private String responsibilities;
    private String achievements;
}



