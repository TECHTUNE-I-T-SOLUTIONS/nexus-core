package com.techtune.nexus.webhook.application;

import com.techtune.nexus.webhook.domain.WebhookOutbox;
import com.techtune.nexus.webhook.persistence.WebhookOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookDeliveryManagementService {

    private final WebhookOutboxRepository webhookOutboxRepository;

    public WebhookDeliveryManagementService(WebhookOutboxRepository webhookOutboxRepository) {
        this.webhookOutboxRepository = webhookOutboxRepository;
    }

    public List<WebhookOutbox> listDeliveries(UUID tenantId, String deliveryStatus) {
        if (deliveryStatus != null) {
            return webhookOutboxRepository.findDeadLetteredByTenant(tenantId);
        }
        return webhookOutboxRepository.findByTenantId(tenantId);
    }

    public WebhookOutbox getDelivery(UUID id) {
        WebhookOutbox delivery = webhookOutboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook delivery not found: " + id));
        return delivery;
    }

    @Transactional
    public WebhookOutbox replayDelivery(UUID id, UUID authenticatedTenantId) {
        WebhookOutbox delivery = webhookOutboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook delivery not found: " + id));

        // Tenant isolation check
        if (delivery.getTenant() == null || !delivery.getTenant().getId().equals(authenticatedTenantId)) {
            throw new SecurityException("Cannot replay delivery for another tenant");
        }

        // Only allow replay of dead-lettered deliveries
        if (!"DEAD_LETTER".equals(delivery.getDeliveryStatus())) {
            throw new IllegalStateException("Only dead-lettered deliveries can be replayed. Current status: " + delivery.getDeliveryStatus());
        }

        // Reset for retry using existing entity methods
        delivery.markForRetry();
        delivery.setErrorMessage(null);
        delivery.setHttpStatus(null);

        return webhookOutboxRepository.save(delivery);
    }
}
