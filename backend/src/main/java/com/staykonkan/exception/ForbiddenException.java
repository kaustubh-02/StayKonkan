package com.staykonkan.exception;

import com.staykonkan.constant.ErrorCode;

/** Thrown when an authenticated user lacks permission for the action. Maps to HTTP 403. */
public class ForbiddenException extends StayKonkanException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
