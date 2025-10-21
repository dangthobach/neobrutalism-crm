package com.neobrutalism.crm.common.exception;

/**
 * Exception thrown when an invalid state transition is attempted
 */
public class InvalidStateTransitionException extends BaseException {

    public InvalidStateTransitionException(String message) {
        super(message, "INVALID_STATE_TRANSITION");
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, "INVALID_STATE_TRANSITION", cause);
    }
}
