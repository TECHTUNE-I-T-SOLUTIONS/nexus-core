package com.techtune.nexus.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null) {
            // For now, we'll implement a simple check
            // In production, this should validate against the database
            try {
                UUID tenantId = validateApiKey(apiKey);
                ApiKeyAuthentication authentication = new ApiKeyAuthentication(tenantId, apiKey);
                authentication.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid API key\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private UUID validateApiKey(String apiKey) {
        // Simple validation for development
        // Format: tenantId:apiKey (e.g., "550e8400-e29b-41d4-a716-446655440000:secret123")
        if (!apiKey.contains(":")) {
            throw new IllegalArgumentException("Invalid API key format");
        }

        String[] parts = apiKey.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid API key format");
        }

        try {
            return UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tenant ID in API key");
        }
    }
}
