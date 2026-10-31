package com.techtune.nexus.webhook.api;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateWebhookEndpointRequest(
        String url,
        String secret,
        Set<String> eventTypes,
        Boolean enabled
) {}
