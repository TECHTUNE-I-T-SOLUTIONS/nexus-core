package com.techtune.nexus.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class ApiKeyAuthentication implements Authentication {

    private final UUID tenantId;
    private final String apiKey;
    private boolean authenticated;

    public ApiKeyAuthentication(UUID tenantId, String apiKey) {
        this.tenantId = tenantId;
        this.apiKey = apiKey;
        this.authenticated = false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return tenantId;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return tenantId.toString();
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
