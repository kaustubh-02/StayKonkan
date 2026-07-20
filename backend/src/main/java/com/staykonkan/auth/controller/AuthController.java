package com.staykonkan.auth.controller;

import com.staykonkan.auth.dto.AuthResponse;
import com.staykonkan.auth.dto.LoginRequest;
import com.staykonkan.auth.dto.RefreshTokenRequest;
import com.staykonkan.auth.dto.RegisterRequest;
import com.staykonkan.auth.service.AuthService;
import com.staykonkan.constant.AppConstants;
import com.staykonkan.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * All endpoints here are listed in SecurityConstants.PUBLIC_ENDPOINTS
 * (Phase 3 foundation, unmodified) via the "/api/v1/auth/**" pattern, so
 * no @PreAuthorize is needed — SecurityConfig already permits these
 * without a token.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/auth")
@Tag(name = "Authentication", description = "Registration, login, and token refresh")
@SecurityRequirements // overrides the global bearer-auth requirement from OpenApiConfig — these endpoints need no token
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new account (USER or OWNER)")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Account created successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new access/refresh token pair")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request), "Token refreshed successfully");
    }
}
