package com.resumeai.section.dto;

import com.resumeai.section.entity.SectionType;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionUpdateItemRequest {

    @NotNull(message = "Section id is required")
    private Long sectionId;

    @NotNull(message = "Section type is required")
    private SectionType sectionType;

    @NotBlank(message = "Title is required")
    private String title;

    private String content;

    @NotNull(message = "Display order is required")
    @JsonAlias("orderIndex")
    private Integer displayOrder;

    @NotNull(message = "Visibility flag is required")
    private Boolean isVisible;

    @NotNull(message = "AI generated flag is required")
    private Boolean aiGenerated;
}
