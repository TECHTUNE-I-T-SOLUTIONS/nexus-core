package com.techtune.nexus.webhook.api;

import com.techtune.nexus.webhook.domain.WebhookEndpoint;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WebhookEndpointResponse(
        UUID id,
        UUID tenantId,
        String url,
        Set<String> eventTypes,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static WebhookEndpointResponse from(WebhookEndpoint endpoint) {
        return new WebhookEndpointResponse(
                endpoint.getId(),
                endpoint.getTenant().getId(),
                endpoint.getUrl(),
                endpoint.getEventTypes(),
                endpoint.isEnabled(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt()
        );
    }
}
