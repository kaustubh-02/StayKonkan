package com.staykonkan.constant;

/**
 * Security-related constants shared across the security package.
 * Centralized here so magic strings never get duplicated/mistyped
 * across filters, providers, and configuration classes.
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_TOKEN_TYPE = "type";

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    public static final String ROLE_PREFIX = "ROLE_";

    /** Public, unauthenticated endpoints — kept in one place so
     *  SecurityConfig and any future gateway/rate-limiter agree. */
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info",
            // Module 10B: called server-to-server by Razorpay, never by a
            // logged-in user — authenticity is instead guaranteed by
            // WebhookSignatureVerifier checking X-Razorpay-Signature
            // against the raw body, not by a JWT.
            "/api/v1/payments/webhook"
    };
}
