package com.staykonkan.security;

import com.staykonkan.constant.SecurityConstants;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the "Authorization: Bearer <token>" header, validates it via
 * JwtTokenProvider, and — if valid — populates the SecurityContext so
 * downstream @PreAuthorize checks and controllers see an authenticated
 * principal.
 *
 * IMPORTANT: this bean is intentionally registered ONLY inside the Spring
 * Security filter chain via {@code SecurityConfig#securityFilterChain}
 * (addFilterBefore). It is still a @Component so it can be constructor-
 * injected into SecurityConfig, but {@link com.staykonkan.config.FilterRegistrationConfig}
 * disables Spring Boot's default auto-registration of it as a *second*,
 * generic servlet filter — without that, the filter would run twice per
 * request (once outside Spring Security, once inside).
 *
 * Depends only on the standard Spring Security UserDetailsService
 * interface, not on a concrete User entity — the Identity module
 * (Phase 4) supplies the real implementation backed by the `users`
 * table from Phase 2.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (token != null && jwtTokenProvider.isTokenValid(token) && jwtTokenProvider.isAccessToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String subject = jwtTokenProvider.getSubject(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(subject);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Ignoring invalid JWT on request {}: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    @SuppressWarnings("unused")
    private List<GrantedAuthority> authoritiesFromToken(String token) {
        return jwtTokenProvider.getRoles(token).stream()
                .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
