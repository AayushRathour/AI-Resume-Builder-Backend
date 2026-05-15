package com.resumeai.auth.exception;

/** Exception type for account suspended workflow failures. */
public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException(String message) {
        super(message);
    }
}



