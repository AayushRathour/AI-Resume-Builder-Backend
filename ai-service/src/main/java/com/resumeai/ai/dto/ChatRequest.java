package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request payload for chat operations. */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {
    private String message;
    private String context; // Optional: can be current page or resume text
}



