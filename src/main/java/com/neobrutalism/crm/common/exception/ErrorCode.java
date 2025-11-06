package com.neobrutalism.crm.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Error codes for business exceptions
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // General errors
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found"),
    INVALID_INPUT("INVALID_INPUT", "Invalid input"),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error"),

    // Authentication errors
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid username or password"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Account is locked"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "Account is disabled"),
    ACCOUNT_EXPIRED("ACCOUNT_EXPIRED", "Account has expired"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token has expired"),
    TOKEN_INVALID("TOKEN_INVALID", "Invalid token"),
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized access"),

    // Authorization errors
    FORBIDDEN("FORBIDDEN", "Access denied"),
    INSUFFICIENT_PERMISSIONS("INSUFFICIENT_PERMISSIONS", "Insufficient permissions"),

    // User errors
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "User already exists"),
    USERNAME_TAKEN("USERNAME_TAKEN", "Username is already taken"),
    EMAIL_TAKEN("EMAIL_TAKEN", "Email is already taken"),

    // Organization errors
    ORGANIZATION_NOT_FOUND("ORGANIZATION_NOT_FOUND", "Organization not found"),

    // Branch errors
    BRANCH_NOT_FOUND("BRANCH_NOT_FOUND", "Branch not found"),

    // Role errors
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found"),
    ROLE_ALREADY_EXISTS("ROLE_ALREADY_EXISTS", "Role already exists"),

    // State transition errors
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", "Invalid state transition"),

    // Validation errors
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation error"),
    INVALID_PASSWORD("INVALID_PASSWORD", "Password does not meet security requirements"),
    DUPLICATE_ENTRY("DUPLICATE_ENTRY", "Duplicate entry");

    private final String code;
    private final String message;
}
