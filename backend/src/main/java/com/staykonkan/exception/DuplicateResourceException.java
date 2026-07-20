package com.staykonkan.exception;

import com.staykonkan.constant.ErrorCode;

/**
 * Thrown for "this already exists" conflicts (duplicate email on
 * registration, later: duplicate property slug, etc.). Maps to HTTP 409.
 * No change to GlobalExceptionHandler was needed — its existing
 * StayKonkanException handler already covers this via ErrorCode.
 */
public class DuplicateResourceException extends StayKonkanException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
