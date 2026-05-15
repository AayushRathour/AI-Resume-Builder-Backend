package com.resumeai.auth.exception;

/** Exception type for otp expired workflow failures. */
public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException(String message) {
        super(message);
    }
}



