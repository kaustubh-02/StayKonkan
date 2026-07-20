package com.staykonkan.config;

import com.staykonkan.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Without this, Spring Boot would auto-register every {@code Filter} bean
 * (including {@link JwtAuthenticationFilter}) as a generic servlet filter
 * IN ADDITION to {@code SecurityConfig} explicitly adding it into the
 * Spring Security chain via {@code addFilterBefore} — causing it to run
 * twice per request. Disabling the generic registration here means it
 * runs exactly once, at the position SecurityConfig places it.
 *
 * {@link com.staykonkan.web.CorrelationIdFilter} deliberately does NOT
 * need this treatment — it is meant to run as an ordinary servlet filter
 * (before Spring Security even executes) and is not added to the
 * security chain anywhere, so no double-registration occurs for it.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
