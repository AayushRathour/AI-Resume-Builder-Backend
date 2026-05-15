package com.resumeai.resume.exception;

/**
 * Raised when a resume record cannot be found.
 */
public class ResumeNotFoundException extends RuntimeException {

    public ResumeNotFoundException(String message) {
        super(message);
    }
}
