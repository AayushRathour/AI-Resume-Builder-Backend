package com.resumeai.auth.exception;

/** Exception type for user not found workflow failures. */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}



