package com.resumeai.resume.exception;

/**
 * Raised when request input violates resume-service rules.
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
