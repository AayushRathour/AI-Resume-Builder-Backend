package com.resumeai.auth.exception;

/** Exception type for invalid otp workflow failures. */
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }
}



