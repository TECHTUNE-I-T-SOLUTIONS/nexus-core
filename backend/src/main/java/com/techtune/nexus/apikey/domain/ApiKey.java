package com.techtune.nexus.apikey.domain;

import com.techtune.nexus.common.domain.UuidIdentity;
import com.techtune.nexus.tenant.domain.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
public class ApiKey extends UuidIdentity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "public_key_id", nullable = false, unique = true, updatable = false)
    private UUID publicKeyId;

    @Column(name = "hashed_secret", nullable = false, length = 255)
    private String hashedSecret;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope", length = 50)
    private Set<String> scopes;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public ApiKey() {
    }

    public ApiKey(Tenant tenant, UUID publicKeyId, String hashedSecret, Set<String> scopes, Instant expiresAt) {
        this.tenant = tenant;
        this.publicKeyId = publicKeyId;
        this.hashedSecret = hashedSecret;
        this.scopes = scopes;
        this.expiresAt = expiresAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public UUID getPublicKeyId() {
        return publicKeyId;
    }

    public String getHashedSecret() {
        return hashedSecret;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    public boolean hasScope(String scope) {
        return scopes != null && (scopes.contains("*") || scopes.contains(scope));
    }
}
