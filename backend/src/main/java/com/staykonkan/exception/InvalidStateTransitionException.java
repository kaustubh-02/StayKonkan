package com.staykonkan.exception;

import com.staykonkan.constant.ErrorCode;

/**
 * Thrown by the Booking module's state machine (Phase 4+) when an illegal
 * status transition is attempted, e.g. COMPLETED -> PENDING. Maps to HTTP 409.
 * Declared in the foundation now because the exception hierarchy must be
 * stable before any module starts throwing from it.
 */
public class InvalidStateTransitionException extends StayKonkanException {

    public InvalidStateTransitionException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}
