package com.techtune.nexus.webhook.api;

import com.techtune.nexus.webhook.domain.WebhookOutbox;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryResponse(
        UUID id,
        UUID tenantId,
        UUID jobId,
        String webhookUrl,
        String eventType,
        String eventVersion,
        String payload,
        Integer retryCount,
        Integer maxRetries,
        Instant nextRetryAt,
        Instant deliveredAt,
        String deliveryStatus,
        Integer httpStatus,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static WebhookDeliveryResponse from(WebhookOutbox outbox) {
        return new WebhookDeliveryResponse(
                outbox.getId(),
                outbox.getTenant() != null ? outbox.getTenant().getId() : null,
                outbox.getJob() != null ? outbox.getJob().getId() : null,
                outbox.getWebhookUrl(),
                outbox.getEventType(),
                outbox.getEventVersion(),
                outbox.getPayload(),
                outbox.getRetryCount(),
                outbox.getMaxRetries(),
                outbox.getNextRetryAt(),
                outbox.getDeliveredAt(),
                outbox.getDeliveryStatus(),
                outbox.getHttpStatus(),
                outbox.getErrorMessage(),
                outbox.getCreatedAt(),
                outbox.getUpdatedAt()
        );
    }
}
