package com.resumeai.notification.exception;

import java.util.UUID;

/** Exception type for resource not found workflow failures. */

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " not found with id: " + id);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}



