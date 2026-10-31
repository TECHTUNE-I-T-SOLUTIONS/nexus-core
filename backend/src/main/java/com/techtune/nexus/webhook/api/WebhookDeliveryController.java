package com.techtune.nexus.webhook.api;

import com.techtune.nexus.webhook.domain.WebhookOutbox;
import com.techtune.nexus.webhook.persistence.WebhookOutboxRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhook-deliveries")
public class WebhookDeliveryController {

    private final WebhookOutboxRepository webhookOutboxRepository;

    public WebhookDeliveryController(WebhookOutboxRepository webhookOutboxRepository) {
        this.webhookOutboxRepository = webhookOutboxRepository;
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

    @GetMapping("/dead-lettered")
    @Transactional(readOnly = true)
    public List<WebhookOutbox> getDeadLetteredDeliveries() {
        UUID tenantId = getAuthenticatedTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return webhookOutboxRepository.findDeadLetteredByTenant(tenantId);
    }

    @PostMapping("/{id}/retry")
    @Transactional
    public WebhookOutbox retryDelivery(@PathVariable UUID id) {
        WebhookOutbox outbox = webhookOutboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook delivery not found: " + id));

        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(outbox.getTenant().getId())) {
            throw new IllegalArgumentException("Cannot access webhook deliveries from other tenants");
        }

        if (!"DEAD_LETTER".equals(outbox.getDeliveryStatus())) {
            throw new IllegalArgumentException("Can only retry dead-lettered deliveries");
        }

        outbox.markForRetry();
        return webhookOutboxRepository.save(outbox);
    }
}
