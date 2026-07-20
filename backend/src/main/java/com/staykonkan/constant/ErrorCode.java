package com.staykonkan.constant;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes returned in every ApiError response body,
 * paired with the HTTP status they map to. Frontend/mobile clients should
 * switch on `code`, never on the free-text `message` (Phase 1, Exception
 * Handling Strategy).
 */
public enum ErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT),
    CONFLICT(HttpStatus.CONFLICT),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
