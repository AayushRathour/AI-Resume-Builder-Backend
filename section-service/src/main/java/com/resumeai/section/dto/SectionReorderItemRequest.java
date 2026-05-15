package com.resumeai.section.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for section reorder item operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionReorderItemRequest {

    @NotNull(message = "Section id is required")
    private Long sectionId;

    @NotNull(message = "Display order is required")
    @JsonAlias("orderIndex")
    private Integer displayOrder;
}



