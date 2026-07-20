package com.staykonkan.exception;

import com.staykonkan.constant.ErrorCode;

/** Thrown when authentication is missing or invalid. Maps to HTTP 401. */
public class UnauthorizedException extends StayKonkanException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
