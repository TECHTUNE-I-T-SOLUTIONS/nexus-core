package com.techtune.nexus.webhook.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateWebhookEndpointRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(max = 2048) String url,
        @NotBlank @Size(min = 16, max = 255) String secret,
        @NotNull Set<String> eventTypes
) {}
