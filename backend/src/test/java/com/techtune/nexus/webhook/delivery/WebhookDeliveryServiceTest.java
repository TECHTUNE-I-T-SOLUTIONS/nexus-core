package com.techtune.nexus.webhook.delivery;

import com.techtune.nexus.webhook.application.WebhookEndpointService;
import com.techtune.nexus.webhook.metrics.WebhookDeliveryMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class WebhookDeliveryServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WebhookEndpointService webhookEndpointService;

    @Mock
    private WebhookDeliveryMetrics metrics;

    private WebhookDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deliveryService = new WebhookDeliveryService(restTemplate, webhookEndpointService, metrics);
    }

    @Test
    void testSignatureGeneration() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        String signature = deliveryService.generateSignature(secret, payload);

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        // Signature should be deterministic
        String signature2 = deliveryService.generateSignature(secret, payload);
        assertEquals(signature, signature2);
    }

    @Test
    void testSignatureChangesWithDifferentPayload() {
        String secret = "my-secret-key-is-16-chars";
        String payload1 = "{\"test\":\"data1\"}";
        String payload2 = "{\"test\":\"data2\"}";

        String signature1 = deliveryService.generateSignature(secret, payload1);
        String signature2 = deliveryService.generateSignature(secret, payload2);

        assertNotEquals(signature1, signature2);
    }

    @Test
    void testSignatureChangesWithDifferentSecret() {
        String secret1 = "my-secret-key-is-16-chars";
        String secret2 = "another-secret-key-16-ch";
        String payload = "{\"test\":\"data\"}";

        String signature1 = deliveryService.generateSignature(secret1, payload);
        String signature2 = deliveryService.generateSignature(secret2, payload);

        assertNotEquals(signature1, signature2);
    }
}
