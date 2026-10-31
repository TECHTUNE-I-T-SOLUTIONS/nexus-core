package com.techtune.nexus.webhook.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.techtune.nexus.common.domain.UuidIdentity;
import com.techtune.nexus.tenant.domain.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint extends UuidIdentity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "secret", nullable = false, length = 255)
    private String secret;

    @Column(name = "event_types", nullable = false, columnDefinition = "TEXT")
    private String eventTypesJson;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "secret_encrypted", nullable = false)
    private Boolean secretEncrypted = false;

    public WebhookEndpoint() {
    }

    public WebhookEndpoint(Tenant tenant, String url, String secret, Set<String> eventTypes) {
        this.tenant = tenant;
        this.url = url;
        this.secret = secret;
        this.eventTypesJson = serializeEventTypes(eventTypes);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getUrl() {
        return url;
    }

    public String getSecret() {
        return secret;
    }

    public Set<String> getEventTypes() {
        return deserializeEventTypes(eventTypesJson);
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Boolean isSecretEncrypted() {
        return secretEncrypted;
    }

    public void setSecretEncrypted(Boolean secretEncrypted) {
        this.secretEncrypted = secretEncrypted;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setEventTypes(Set<String> eventTypes) {
        this.eventTypesJson = serializeEventTypes(eventTypes);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    private String serializeEventTypes(Set<String> eventTypes) {
        if (eventTypes == null) {
            return "[]";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(eventTypes);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Set<String> deserializeEventTypes(String json) {
        if (json == null || json.isEmpty()) {
            return Set.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode node = mapper.readTree(json);
            return mapper.convertValue(node, 
                mapper.getTypeFactory().constructCollectionType(Set.class, String.class));
        } catch (Exception e) {
            return Set.of();
        }
    }
}
