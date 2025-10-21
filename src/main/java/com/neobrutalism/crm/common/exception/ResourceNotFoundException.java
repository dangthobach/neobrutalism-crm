package com.neobrutalism.crm.common.exception;

/**
 * Exception thrown when a resource is not found
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s not found with id: %s", resourceName, id), "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, String field, Object value) {
        super(String.format("%s not found with %s: %s", resourceName, field, value), "RESOURCE_NOT_FOUND");
    }
}
