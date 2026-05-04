package com.resumeai.jobmatch.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkRequest {

    @NotNull
    private UUID matchId;

    @NotNull
    private Boolean bookmarked;
}
