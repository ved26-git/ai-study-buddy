
package com.aistudybuddy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(
            JwtTokenProvider tokenProvider,
            TokenBlacklistService tokenBlacklistService) {

        this.tokenProvider = tokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = getJwtFromRequest(request);

        if (token != null && !token.isBlank()) {

            // Step 1: Validate JWT signature and expiration first.
            if (tokenProvider.validateToken(token)) {

                try {
                    // Step 2: Check persistent revocation status.
                    boolean blacklisted =
                            tokenBlacklistService.isBlacklisted(token);

                    if (blacklisted) {
                        SecurityContextHolder.clearContext();
                        response.sendError(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Token has been revoked."
                        );
                        return;
                    }

                    // Step 3: Authenticate only after both checks succeed.
                    String email = tokenProvider.getEmailFromToken(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    Collections.emptyList()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                } catch (IllegalStateException ex) {

                    // Database/revocation verification failed.
                    SecurityContextHolder.clearContext();

                    response.setStatus(
                            HttpStatus.SERVICE_UNAVAILABLE.value()
                    );
                    response.setContentType(
                            MediaType.APPLICATION_JSON_VALUE
                    );
                    response.getWriter().write(
                            "{\"error\":\"Authentication temporarily unavailable. Please try again.\"}"
                    );
                    return;
                }

            } else {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {

        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null
                && bearerToken.startsWith("Bearer ")) {

            return bearerToken.substring(7).trim();
        }

        return null;
    }
}
