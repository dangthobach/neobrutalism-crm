package com.neobrutalism.crm.common.exception;

/**
 * Unauthorized Exception
 * Thrown when user is not authenticated or lacks necessary permissions
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message, errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getCode());
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, "UNAUTHORIZED", cause);
    }
}
