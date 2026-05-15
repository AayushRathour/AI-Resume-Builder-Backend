package com.resumeai.auth.exception;

/** Exception type for account deleted workflow failures. */
public class AccountDeletedException extends RuntimeException {
    public AccountDeletedException(String message) {
        super(message);
    }
}



