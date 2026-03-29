package com.legalrag.config;

import com.legalrag.auth.JwtService;
import com.legalrag.auth.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — runs exactly once per HTTP request.
 *
 * Execution order per request:
 *   1. Extract the Authorization header.
 *   2. If missing or not "Bearer ...", pass through unchanged (anonymous).
 *   3. Parse the email from the token via JwtService.
 *   4. Load the UserDetails from the database via UserDetailsServiceImpl.
 *   5. Validate the token (signature + expiry + subject match).
 *   6. If valid, set the authentication on the SecurityContext.
 *   7. Continue the filter chain regardless — SecurityConfig decides
 *      which endpoints require authentication.
 *
 * Why OncePerRequestFilter?
 *   In Servlet filter chains, a filter can be invoked multiple times
 *   per request (e.g. during async dispatch or error forwarding).
 *   OncePerRequestFilter guarantees exactly one execution per logical
 *   request, which is critical for security filters.
 *
 * Why not set auth if SecurityContext already has one?
 *   If another filter already authenticated the request (unlikely here,
 *   but defensive), we leave it alone. Never overwrite an existing
 *   authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER   = "Authorization";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTH_HEADER);
       

        // ── 1. No Authorization header or not a Bearer token → skip. ──────────
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 2. Extract token (strip "Bearer " prefix). ────────────────────────
        final String token = authHeader.substring(BEARER_PREFIX.length());

        // ── 3. Parse email from token claims. ────────────────────────────────
        //    extractEmail() throws JwtException on malformed/expired tokens.
        //    We catch broadly so a bad token results in an anonymous request
        //    (which SecurityConfig will reject for protected routes) rather
        //    than a 500 error.
        final String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            log.debug("Could not extract email from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ── 4. Only proceed if we have an email AND no auth is set yet. ───────
        if (email != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // ── 5. Load UserDetails (hits the database once per request). ─────
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // ── 6. Validate token against the loaded user. ────────────────────
            if (jwtService.isTokenValid(token, userDetails.getUsername())) {

                // ── 7. Build authentication token and set on SecurityContext. ──
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                        // credentials — null post-auth
                                userDetails.getAuthorities()
                        );

                // Attach request details (IP, session ID) for audit logging.
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated request for user: {}", email);
            }
        }

        // ── 8. Always continue the filter chain. ─────────────────────────────
        filterChain.doFilter(request, response);
        
    }
}