package com.resumeai.auth.exception;

/** Exception type for unauthorized access workflow failures. */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}



