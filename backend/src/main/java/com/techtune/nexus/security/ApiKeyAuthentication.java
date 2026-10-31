package com.techtune.nexus.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApiKeyAuthentication implements Authentication {

    private final UUID tenantId;
    private final UUID publicKeyId; // Store only public key ID, not the full secret
    private final Set<String> scopes;
    private boolean authenticated;

    public ApiKeyAuthentication(UUID tenantId, UUID publicKeyId) {
        this(tenantId, publicKeyId, Collections.emptySet());
    }

    public ApiKeyAuthentication(UUID tenantId, UUID publicKeyId, Set<String> scopes) {
        this.tenantId = tenantId;
        this.publicKeyId = publicKeyId;
        this.scopes = scopes;
        this.authenticated = false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return scopes.stream()
                .map(scope -> (GrantedAuthority) () -> "SCOPE_" + scope)
                .collect(Collectors.toList());
    }

    @Override
    public Object getCredentials() {
        // Return only the public key ID, not the secret
        return publicKeyId;
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

    public UUID getPublicKeyId() {
        return publicKeyId;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}
