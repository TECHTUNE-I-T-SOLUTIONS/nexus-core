package com.techtune.nexus.webhook.application;

import com.techtune.nexus.tenant.application.TenantService;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.webhook.SSRFProtection;
import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import com.techtune.nexus.webhook.encryption.SecretEncryptionService;
import com.techtune.nexus.webhook.persistence.WebhookEndpointRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.UUID;

@Service
public class WebhookEndpointService {

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final TenantService tenantService;
    private final SecretEncryptionService encryptionService;
    private final boolean encryptionEnabled;

    public WebhookEndpointService(
            WebhookEndpointRepository webhookEndpointRepository,
            TenantService tenantService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SecretEncryptionService encryptionService,
            @Value("${nexus.webhook.encryption.enabled:false}") boolean encryptionEnabled) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.tenantService = tenantService;
        this.encryptionService = encryptionService;
        this.encryptionEnabled = encryptionEnabled;
    }

    private UUID getAuthenticatedTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UUID) {
                return (UUID) principal;
            }
        }
        return null;
    }

    @Transactional
    public WebhookEndpoint createWebhookEndpoint(UUID tenantId, String url, String secret, Set<String> eventTypes) {
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(tenantId)) {
            throw new IllegalArgumentException("Cannot create webhook endpoints for other tenants");
        }

        validateWebhookUrl(url);
        validateSecret(secret);

        String secretToStore = encryptionEnabled && encryptionService != null ? encryptionService.encrypt(secret) : secret;
        boolean isEncrypted = encryptionEnabled && encryptionService != null;

        Tenant tenant = tenantService.findTenantById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        WebhookEndpoint endpoint = new WebhookEndpoint(tenant, url, secretToStore, eventTypes);
        endpoint.setSecretEncrypted(isEncrypted);
        return webhookEndpointRepository.save(endpoint);
    }

    @Transactional(readOnly = true)
    public WebhookEndpoint getWebhookEndpoint(UUID id) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found: " + id));

        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(endpoint.getTenant().getId())) {
            throw new IllegalArgumentException("Cannot access webhook endpoints from other tenants");
        }

        return endpoint;
    }

    @Transactional(readOnly = true)
    public java.util.List<WebhookEndpoint> listWebhookEndpoints(UUID tenantId) {
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null) {
            tenantId = authenticatedTenantId;
        }

        if (tenantId != null) {
            return webhookEndpointRepository.findByTenantId(tenantId);
        }
        return webhookEndpointRepository.findAll();
    }

    @Transactional(readOnly = true)
    public java.util.List<WebhookEndpoint> findEnabledEndpointsForEvent(UUID tenantId, String eventType) {
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null) {
            tenantId = authenticatedTenantId;
        }

        if (tenantId == null) {
            return java.util.List.of();
        }

        java.util.List<WebhookEndpoint> allEnabled = webhookEndpointRepository.findEnabledByTenantId(tenantId);
        
        // Filter by event type in application layer since eventTypes is stored as JSON
        if (eventType == null) {
            return allEnabled;
        }
        
        return allEnabled.stream()
                .filter(endpoint -> endpoint.getEventTypes().contains(eventType))
                .toList();
    }

    @Transactional
    public WebhookEndpoint updateWebhookEndpoint(UUID id, String url, String secret, Set<String> eventTypes, Boolean enabled) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found: " + id));

        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(endpoint.getTenant().getId())) {
            throw new IllegalArgumentException("Cannot modify webhook endpoints from other tenants");
        }

        if (url != null) {
            validateWebhookUrl(url);
            endpoint.setUrl(url);
        }
        if (secret != null) {
            validateSecret(secret);
            String secretToStore = encryptionEnabled && encryptionService != null ? encryptionService.encrypt(secret) : secret;
            boolean isEncrypted = encryptionEnabled && encryptionService != null;
            endpoint.setSecret(secretToStore);
            endpoint.setSecretEncrypted(isEncrypted);
        }
        if (eventTypes != null) {
            endpoint.setEventTypes(eventTypes);
        }
        if (enabled != null) {
            endpoint.setEnabled(enabled);
        }

        return webhookEndpointRepository.save(endpoint);
    }

    @Transactional
    public void deleteWebhookEndpoint(UUID id) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found: " + id));

        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(endpoint.getTenant().getId())) {
            throw new IllegalArgumentException("Cannot delete webhook endpoints from other tenants");
        }

        webhookEndpointRepository.delete(endpoint);
    }

    private void validateWebhookUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Webhook URL cannot be blank");
        }

        // Check for user-info component (credentials in URL)
        if (SSRFProtection.hasUserInfo(url)) {
            throw new IllegalArgumentException("Webhook URL cannot contain user credentials");
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            
            if (!SSRFProtection.isAllowedScheme(scheme)) {
                throw new IllegalArgumentException("Webhook URL must use HTTP or HTTPS scheme");
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Webhook URL must have a valid host");
            }

            // Comprehensive SSRF protection including DNS resolution
            if (!SSRFProtection.isSafeHost(host)) {
                throw new IllegalArgumentException("Webhook URL cannot point to localhost, private IPs, or blocked addresses");
            }

            // Reject non-standard ports for safety
            int port = uri.getPort();
            if (!SSRFProtection.isValidPort(port)) {
                throw new IllegalArgumentException("Webhook URL must use a valid port (80-65535)");
            }

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid webhook URL: " + e.getMessage());
        }
    }

    private void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Webhook secret cannot be blank");
        }
        if (secret.length() < 16) {
            throw new IllegalArgumentException("Webhook secret must be at least 16 characters");
        }
    }

    /**
     * Decrypts the stored secret for use in webhook delivery.
     * 
     * @param encryptedSecret The encrypted secret from the database
     * @return The plaintext secret
     */
    public String decryptSecret(String encryptedSecret) {
        if (!encryptionEnabled || encryptionService == null) {
            return encryptedSecret;
        }
        return encryptionService.decrypt(encryptedSecret);
    }
}
