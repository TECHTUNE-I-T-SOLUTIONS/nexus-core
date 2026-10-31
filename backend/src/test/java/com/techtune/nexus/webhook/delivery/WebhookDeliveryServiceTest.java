package com.techtune.nexus.webhook.delivery;

import com.techtune.nexus.webhook.application.WebhookEndpointService;
import com.techtune.nexus.webhook.metrics.WebhookDeliveryMetrics;
import com.techtune.nexus.webhook.signing.WebhookSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

class WebhookDeliveryServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WebhookEndpointService webhookEndpointService;

    @Mock
    private WebhookDeliveryMetrics metrics;

    @Mock
    private WebhookSignatureService signatureService;

    private WebhookDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deliveryService = new WebhookDeliveryService(restTemplate, webhookEndpointService, metrics, signatureService);
    }
}
