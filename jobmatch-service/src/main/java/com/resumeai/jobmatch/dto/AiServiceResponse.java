package com.resumeai.jobmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for ai service operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiServiceResponse<T> {
    private String status;
    private String message;
    private T data;
}



