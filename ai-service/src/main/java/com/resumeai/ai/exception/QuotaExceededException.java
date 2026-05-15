package com.resumeai.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception type for quota exceeded workflow failures. */

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}



