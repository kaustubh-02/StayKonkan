package com.staykonkan.exception;

import com.staykonkan.constant.ErrorCode;
import lombok.Getter;

/**
 * Base of the application's exception hierarchy. Every custom exception
 * carries an {@link ErrorCode} so {@link GlobalExceptionHandler} can map
 * it to the correct HTTP status without a giant if/else chain, and so
 * frontend clients get a stable, machine-readable error code.
 */
@Getter
public class StayKonkanException extends RuntimeException {

    private final ErrorCode errorCode;

    public StayKonkanException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StayKonkanException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
