package com.staykonkan.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.staykonkan.constant.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body returned by {@link com.staykonkan.exception.GlobalExceptionHandler}
 * for every failure case, matching the contract defined in Phase 1,
 * Section 21 (Exception Handling Strategy).
 *
 * Example JSON:
 * {
 *   "timestamp": "2026-07-19T10:15:00Z",
 *   "status": 409,
 *   "error": "INVALID_STATE_TRANSITION",
 *   "message": "Cannot move booking from COMPLETED to PENDING",
 *   "path": "/api/v1/booking-requests/123/status",
 *   "traceId": "a1b2c3..."
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final Instant timestamp;
    private final int status;
    private final ErrorCode error;
    private final String message;
    private final String path;
    private final String traceId;

    /** Present only for validation failures — one entry per invalid field. */
    private final List<FieldViolation> fieldErrors;

    @Getter
    @Builder
    public static class FieldViolation {
        private final String field;
        private final String message;
    }
}
