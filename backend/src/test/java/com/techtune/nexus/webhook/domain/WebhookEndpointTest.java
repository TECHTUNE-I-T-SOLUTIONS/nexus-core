package com.techtune.nexus.webhook.domain;

import com.techtune.nexus.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WebhookEndpointTest {

    @Test
    void testWebhookEndpointCreation() {
        Tenant tenant = new Tenant("test-tenant");
        String url = "https://example.com/webhook";
        String secret = "my-secret-key-is-16-chars";
        Set<String> eventTypes = Set.of("job.completed", "job.failed");

        WebhookEndpoint endpoint = new WebhookEndpoint(tenant, url, secret, eventTypes);

        assertNotNull(endpoint);
        assertEquals(tenant, endpoint.getTenant());
        assertEquals(url, endpoint.getUrl());
        assertEquals(secret, endpoint.getSecret());
        assertEquals(eventTypes, endpoint.getEventTypes());
        assertTrue(endpoint.isEnabled());
    }

    @Test
    void testWebhookEndpointEventTypesSerialization() {
        Tenant tenant = new Tenant("test-tenant");
        Set<String> eventTypes = Set.of("job.completed", "job.failed", "job.cancelled");

        WebhookEndpoint endpoint = new WebhookEndpoint(tenant, "https://example.com/webhook", "secret-16-chars!!", eventTypes);

        assertEquals(eventTypes, endpoint.getEventTypes());
    }

    @Test
    void testWebhookEndpointEmptyEventTypes() {
        Tenant tenant = new Tenant("test-tenant");

        WebhookEndpoint endpoint = new WebhookEndpoint(tenant, "https://example.com/webhook", "secret-16-chars!!", Set.of());

        assertNotNull(endpoint.getEventTypes());
        assertTrue(endpoint.getEventTypes().isEmpty());
    }

    @Test
    void testWebhookEndpointSetters() {
        Tenant tenant = new Tenant("test-tenant");
        WebhookEndpoint endpoint = new WebhookEndpoint();

        endpoint.setTenant(tenant);
        endpoint.setUrl("https://example.com/webhook");
        endpoint.setSecret("secret-16-chars!!");
        endpoint.setEventTypes(Set.of("job.completed"));
        endpoint.setEnabled(false);

        assertEquals(tenant, endpoint.getTenant());
        assertEquals("https://example.com/webhook", endpoint.getUrl());
        assertEquals("secret-16-chars!!", endpoint.getSecret());
        assertEquals(Set.of("job.completed"), endpoint.getEventTypes());
        assertFalse(endpoint.isEnabled());
    }
}
