package com.staykonkan.security;

import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Real UserDetailsService, backed by the users table. This replaces the
 * Phase 3 placeholder (DefaultUserDetailsService, deleted — see the
 * Module 1 delivery notes for why keeping both would break startup).
 *
 * Login is by email (see AuthServiceImpl), so "username" here always
 * means email — nowhere in the security layer conflates the two, this
 * class is the single place that decision is made.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for email: " + email));
        return new SecurityUserPrincipal(user);
    }
}
