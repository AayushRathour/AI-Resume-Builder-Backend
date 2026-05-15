package com.resumeai.section.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for section visibility operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionVisibilityRequest {

    @NotNull(message = "Visibility flag is required")
    private Boolean isVisible;
}



