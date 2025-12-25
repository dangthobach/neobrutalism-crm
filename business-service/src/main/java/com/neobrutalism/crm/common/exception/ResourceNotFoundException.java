package com.neobrutalism.crm.common.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ResourceNotFoundException forResource(String resourceType, Object id) {
        return new ResourceNotFoundException(String.format("%s not found with id: %s", resourceType, id));
    }

    public static ResourceNotFoundException forResourceByField(String resourceType, String field, Object value) {
        return new ResourceNotFoundException(String.format("%s not found with %s: %s", resourceType, field, value));
    }
}