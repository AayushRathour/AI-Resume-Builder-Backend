package com.resumeai.auth.exception;

/** Exception type for email verification workflow failures. */
public class EmailVerificationException extends RuntimeException {
    public EmailVerificationException(String message) {
        super(message);
    }
}



