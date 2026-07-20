package com.staykonkan.auth.service.impl;

import com.staykonkan.auth.dto.AuthResponse;
import com.staykonkan.auth.dto.LoginRequest;
import com.staykonkan.auth.dto.RefreshTokenRequest;
import com.staykonkan.auth.dto.RegisterRequest;
import com.staykonkan.auth.service.AuthService;
import com.staykonkan.exception.DuplicateResourceException;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.UnauthorizedException;
import com.staykonkan.security.JwtProperties;
import com.staykonkan.security.JwtTokenProvider;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.entity.UserStatus;
import com.staykonkan.user.mapper.UserMapper;
import com.staykonkan.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Login is by email; JwtTokenProvider's "subject" claim is always the
 * email (see UserDetailsServiceImpl — that's the single place this
 * convention is documented and enforced).
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(UserRepository userRepository,
                            UserMapper userMapper,
                            PasswordEncoder passwordEncoder,
                            AuthenticationManager authenticationManager,
                            JwtTokenProvider jwtTokenProvider,
                            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            // Admin accounts are provisioned out-of-band (DB seed / an
            // existing admin promoting a user), never via public
            // self-registration — this is a security boundary, not a
            // validation nicety, so it lives in the service layer rather
            // than being expressible as a "valid" DTO state.
            throw new ForbiddenException("Cannot self-register as ADMIN");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("An account with this phone number already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        return issueTokens(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Delegates to Spring Security's AuthenticationManager (wired in
        // SecurityConfig against UserDetailsServiceImpl + BCrypt) rather
        // than comparing passwords by hand, so lockout/hidden-user-not-found
        // behavior stays centralized in one place (Phase 3 foundation).
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword()));

        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return issueTokens(user);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtTokenProvider.isTokenValid(token)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
        if (jwtTokenProvider.isAccessToken(token)) {
            throw new UnauthorizedException("An access token was supplied where a refresh token was expected");
        }

        Long userId = jwtTokenProvider.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }

        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        List<String> roles = List.of(user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInSeconds(jwtProperties.getAccessTokenExpirationMinutes() * 60)
                .user(userMapper.toProfileResponse(user))
                .build();
    }
}
