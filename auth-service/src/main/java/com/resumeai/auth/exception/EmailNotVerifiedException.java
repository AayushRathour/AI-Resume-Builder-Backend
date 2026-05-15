package com.resumeai.auth.exception;

/** Exception type for email not verified workflow failures. */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}



