package com.staykonkan.config;

import com.staykonkan.constant.SecurityConstants;
import com.staykonkan.security.CustomAccessDeniedHandler;
import com.staykonkan.security.JwtAuthenticationEntryPoint;
import com.staykonkan.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Central Spring Security wiring. Stateless (JWT-only) API security —
 * no server-side sessions, no CSRF token needed since we never rely on
 * cookies for authentication (Phase 1 Security Architecture).
 *
 * Business-endpoint authorization rules (e.g. only ADMIN can confirm a
 * booking) are added incrementally as each module's controllers are
 * built, using @PreAuthorize at the method level — this class only
 * establishes the perimeter: what's public vs. what requires a valid
 * JWT at all.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                           CustomAccessDeniedHandler customAccessDeniedHandler,
                           CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless JWT API — CSRF protection defends cookie-based session
            // auth and does not apply here; disabling it is intentional,
            // not an oversight (Phase 1 Security Architecture).
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )

            // Secure headers — OWASP baseline hardening.
            // X-Content-Type-Options: nosniff ships enabled by default in
            // Spring Security and needs no explicit configuration here.
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
            )

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Wires Spring Security's standard DAO-based authentication against
     * whichever UserDetailsService bean is active (the temporary
     * DefaultUserDetailsService now, the Identity module's real one from
     * Phase 4 onward) and our BCrypt encoder. Used by the future
     * /api/v1/auth/login endpoint to authenticate email+password.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                              PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Do not reveal whether the failure was a bad username or bad
        // password — both surface as the same BadCredentialsException,
        // which GlobalExceptionHandler maps to one generic message
        // (OWASP: prevents user-enumeration via login errors).
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }
}
