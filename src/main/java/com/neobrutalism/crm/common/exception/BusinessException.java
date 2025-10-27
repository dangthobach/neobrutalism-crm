package com.neobrutalism.crm.common.exception;

/**
 * Business Exception
 * Thrown when business logic validation fails
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(message, "BUSINESS_ERROR");
    }

    public BusinessException(String message, String errorCode) {
        super(message, errorCode);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, "BUSINESS_ERROR", cause);
    }

    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
