package com.resumeai.auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Fallback behavior for resume service call failures. */
@Component
@Slf4j
public class ResumeServiceClientFallback implements ResumeServiceClient {

    @Override
    public Long countResumes(boolean internalCall) {
        log.warn("ResumeServiceClient fallback for countResumes");
        return 0L;
    }
}





