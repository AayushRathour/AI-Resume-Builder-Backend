package com.resumeai.ai.exception;

/** Exception type for request not found workflow failures. */

public class RequestNotFoundException extends RuntimeException {
    public RequestNotFoundException(String message) {
        super(message);
    }
}



