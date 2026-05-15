package com.resumeai.template.exception;

/** Exception type for template not found workflow failures. */

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(String message) {
        super(message);
    }
}



