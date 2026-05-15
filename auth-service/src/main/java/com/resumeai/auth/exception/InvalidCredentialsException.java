package com.resumeai.auth.exception;

/** Exception type for invalid credentials workflow failures. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}



