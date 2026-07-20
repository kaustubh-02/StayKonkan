package com.staykonkan.exception;

import com.staykonkan.constant.ErrorCode;

/** Thrown for business-rule validation failures beyond bean validation. Maps to HTTP 400. */
public class ValidationException extends StayKonkanException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }
}
