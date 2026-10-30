package com.techtune.nexus.security;

import com.techtune.nexus.apikey.application.ApiKeyService;
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
    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null) {
            try {
                // Format: publicKeyId:secret
                String[] parts = apiKey.split(":", 2);
                if (parts.length != 2) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid API key format\"}");
                    return;
                }

                UUID publicKeyId = UUID.fromString(parts[0]);
                String secret = parts[1];

                var validationResult = apiKeyService.validateApiKey(publicKeyId, secret);
                
                if (validationResult.valid()) {
                    ApiKeyAuthentication authentication = new ApiKeyAuthentication(
                            validationResult.tenantId(), 
                            apiKey,
                            validationResult.scopes()
                    );
                    authentication.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"" + validationResult.error() + "\"}");
                    return;
                }
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid API key\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
