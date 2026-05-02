package com.resumeai.section.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class BulkSectionUpdateRequest {

    @Valid
    @NotEmpty(message = "Sections list cannot be empty")
    private List<SectionUpdateItemRequest> sections;
}
