package com.staykonkan.exception;

import com.staykonkan.constant.ErrorCode;
import com.staykonkan.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Single place where every exception in the application becomes an
 * {@link ApiError} response, per Phase 1 Section 21 (Exception Handling
 * Strategy). Business modules should throw the typed exceptions from
 * {@code com.staykonkan.exception} rather than generic RuntimeExceptions,
 * so they land in the specific handlers below with the right HTTP status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- Domain exceptions (already carry the correct ErrorCode) --------

    @ExceptionHandler(StayKonkanException.class)
    public ResponseEntity<ApiError> handleDomainException(StayKonkanException ex, HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), request, null);
    }

    // ---- Bean Validation (@Valid on request DTOs) ------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ApiError.FieldViolation.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "One or more fields failed validation", request, violations);
    }

    // ---- Spring Security ---------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(ErrorCode.UNAUTHORIZED, "Invalid email or password", request, null);
    }

    /**
     * Catches every other AuthenticationException subtype — most
     * importantly DisabledException, thrown by DaoAuthenticationProvider
     * when SecurityUserPrincipal.isEnabled() is false (i.e. a SUSPENDED
     * or DELETED account tries to log in). Without this handler such
     * attempts fell through to the generic 500 handler below, which is
     * both wrong (it's a 401, not a server error) and confusing to the
     * client. BadCredentialsException above remains the more specific
     * match for wrong-password cases.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        return build(ErrorCode.UNAUTHORIZED, "Authentication failed. Your account may be inactive — please contact support.",
                request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN, "You do not have permission to perform this action", request, null);
    }

    // ---- Database ------------------------------------------------------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        // Never leak raw SQL/constraint names to the client (OWASP: information exposure)
        return build(ErrorCode.CONFLICT, "The request could not be completed due to a data conflict", request, null);
    }

    // ---- Fallback --------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Full exception is logged server-side (with traceId) but never returned to the client.
        return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ApiError> build(ErrorCode errorCode, String message, HttpServletRequest request,
                                            List<ApiError.FieldViolation> fieldErrors) {
        HttpStatus status = errorCode.getHttpStatus();
        ApiError body = ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .traceId(MDC.get("traceId"))
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
