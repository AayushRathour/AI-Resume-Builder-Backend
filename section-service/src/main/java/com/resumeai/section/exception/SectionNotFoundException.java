package com.resumeai.section.exception;

/** Exception type for section not found workflow failures. */

public class SectionNotFoundException extends RuntimeException {

    public SectionNotFoundException(String message) {
        super(message);
    }
}



