package com.techtune.nexus.webhook.delivery;

import com.techtune.nexus.webhook.application.WebhookEndpointService;
import com.techtune.nexus.webhook.metrics.WebhookDeliveryMetrics;
import com.techtune.nexus.webhook.signing.WebhookSignatureService;
import org.mockito.Mock;
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
}
