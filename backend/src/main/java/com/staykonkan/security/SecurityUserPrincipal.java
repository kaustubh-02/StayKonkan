package com.staykonkan.security;

import com.staykonkan.constant.SecurityConstants;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security's view of a User. This is what JwtAuthenticationFilter
 * places into the SecurityContext, and what AuditorAwareImpl reads back
 * out to resolve created_by/updated_by (Phase 3 foundation).
 *
 * Deliberately does NOT hold a live reference to the User entity across
 * a whole request beyond what's needed here — it copies the fields it
 * needs at construction time, so it stays a lightweight, detachable
 * security record rather than a leaking a lazy-loaded JPA entity into
 * the security context.
 */
public class SecurityUserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public SecurityUserPrincipal(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.getStatus() == UserStatus.ACTIVE;
        this.authorities = List.of(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + user.getRole().name()));
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
