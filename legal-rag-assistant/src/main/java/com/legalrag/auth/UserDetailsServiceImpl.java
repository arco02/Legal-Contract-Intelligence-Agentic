package com.legalrag.auth;

import com.legalrag.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Bridge between Spring Security's authentication machinery and our User entity.
 *
 * Spring Security calls loadUserByUsername() in two situations:
 *   1. During form-based login (not used here — we use JWT).
 *   2. When JwtAuthFilter resolves a principal from a validated token:
 *      it calls this method with the email extracted from the JWT subject,
 *      gets back a UserDetails, sets it on the SecurityContext.
 *
 * We use Spring Security's built-in User builder (org.springframework.security
 * .core.userdetails.User) to wrap our entity. This keeps the User JPA entity
 * free of Spring Security interfaces and their lifecycle assumptions.
 *
 * Roles: every user gets ROLE_USER. There is no admin role in this system.
 * Extend this if you add role-based access control later.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by email (the "username" in Spring Security terminology).
     *
     * @param email the email extracted from the JWT subject claim
     * @return a UserDetails wrapping the stored email and password hash
     * @throws UsernameNotFoundException if no user with this email exists
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        com.legalrag.user.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));

        // Spring Security's built-in User builder.
        // passwordHash is already BCrypt-encoded — Spring Security will use
        // PasswordEncoder.matches() to verify it during credential checks.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }
}
