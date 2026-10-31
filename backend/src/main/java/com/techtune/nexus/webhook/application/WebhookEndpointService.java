package com.techtune.nexus.webhook.application;

import com.techtune.nexus.tenant.application.TenantService;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import com.techtune.nexus.webhook.persistence.WebhookEndpointRepository;
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

    public WebhookEndpointService(WebhookEndpointRepository webhookEndpointRepository, TenantService tenantService) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.tenantService = tenantService;
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

        Tenant tenant = tenantService.findTenantById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        WebhookEndpoint endpoint = new WebhookEndpoint(tenant, url, secret, eventTypes);
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
            endpoint.setSecret(secret);
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

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("Webhook URL must use HTTP or HTTPS scheme");
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Webhook URL must have a valid host");
            }

            // Prevent SSRF by rejecting localhost and private IP ranges
            if (isLoopbackOrPrivate(host)) {
                throw new IllegalArgumentException("Webhook URL cannot point to localhost or private IP addresses");
            }

            // Reject non-standard ports for safety
            int port = uri.getPort();
            if (port != -1 && (port < 80 || port > 65535)) {
                throw new IllegalArgumentException("Webhook URL must use a valid port (80-65535)");
            }

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid webhook URL: " + e.getMessage());
        }
    }

    private boolean isLoopbackOrPrivate(String host) {
        // Check for localhost variants
        if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1")) {
            return true;
        }

        // Check for private IP ranges (simplified check)
        // In production, this should use InetAddress.getByName() for proper IP validation
        return host.startsWith("10.") || 
               host.startsWith("192.168.") || 
               host.startsWith("172.16.") || 
               host.startsWith("172.17.") || 
               host.startsWith("172.18.") || 
               host.startsWith("172.19.") || 
               host.startsWith("172.20.") || 
               host.startsWith("172.21.") || 
               host.startsWith("172.22.") || 
               host.startsWith("172.23.") || 
               host.startsWith("172.24.") || 
               host.startsWith("172.25.") || 
               host.startsWith("172.26.") || 
               host.startsWith("172.27.") || 
               host.startsWith("172.28.") || 
               host.startsWith("172.29.") || 
               host.startsWith("172.30.") || 
               host.startsWith("172.31.");
    }

    private void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Webhook secret cannot be blank");
        }
        if (secret.length() < 16) {
            throw new IllegalArgumentException("Webhook secret must be at least 16 characters");
        }
    }
}
