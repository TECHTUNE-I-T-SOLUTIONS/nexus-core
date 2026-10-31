package com.techtune.nexus.webhook.delivery;

import com.techtune.nexus.webhook.SSRFProtection;
import com.techtune.nexus.webhook.application.WebhookEndpointService;
import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import com.techtune.nexus.webhook.domain.WebhookOutbox;
import com.techtune.nexus.webhook.metrics.WebhookDeliveryMetrics;
import com.techtune.nexus.webhook.signing.WebhookSignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;

@Service
public class WebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;

    private final RestTemplate restTemplate;
    private final WebhookEndpointService webhookEndpointService;
    private final WebhookDeliveryMetrics metrics;
    private final WebhookSignatureService signatureService;

    @Autowired
    public WebhookDeliveryService(WebhookEndpointService webhookEndpointService,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false) WebhookDeliveryMetrics metrics,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false) WebhookSignatureService signatureService) {
        this.webhookEndpointService = webhookEndpointService;
        this.metrics = metrics != null ? metrics : new NoOpMetrics();
        this.signatureService = signatureService != null ? signatureService : new WebhookSignatureService();
        this.restTemplate = createRestTemplate();
    }

    // Constructor for testing with mocked RestTemplate
    public WebhookDeliveryService(RestTemplate restTemplate, WebhookEndpointService webhookEndpointService, WebhookDeliveryMetrics metrics, WebhookSignatureService signatureService) {
        this.restTemplate = restTemplate;
        this.webhookEndpointService = webhookEndpointService;
        this.metrics = metrics != null ? metrics : new NoOpMetrics();
        this.signatureService = signatureService != null ? signatureService : new WebhookSignatureService();
    }

    // No-op metrics implementation for when metrics is not available
    private static class NoOpMetrics extends WebhookDeliveryMetrics {
        public NoOpMetrics() {
            super(io.micrometer.core.instrument.Metrics.globalRegistry);
        }

        @Override
        public void recordDeliverySuccess() {}
        @Override
        public void recordDeliveryFailure() {}
        @Override
        public void recordDeliveryRetry() {}
        @Override
        public void recordDeadLetterTransition() {}
        @Override
        public void recordLeaseRecovery() {}
        @Override
        public void recordDeliveryLatency(java.time.Duration latency) {}
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT_SECONDS * 1000);
        factory.setReadTimeout(RESPONSE_TIMEOUT_SECONDS * 1000);
        RestTemplate restTemplate = new RestTemplate(factory);
        // Disable automatic redirect following to prevent SSRF attacks via redirects
        // Note: SimpleClientHttpRequestFactory doesn't have a built-in redirect disable option
        // We handle 3xx responses as errors in the delivery method
        return restTemplate;
    }

    public DeliveryResult deliver(WebhookEndpoint endpoint, WebhookOutbox outbox) {
        try {
            // Validate the target URL at connection time (DNS rebinding protection)
            validateDeliveryTarget(endpoint.getUrl());

            // Decrypt secret for signature generation
            String secret = webhookEndpointService.decryptSecret(endpoint.getSecret());

            String body = outbox.getPayload();
            String signature = signatureService.generateSignature(secret, body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String outboxId = outbox.getId() != null ? outbox.getId().toString() : "unknown";
            headers.set("X-Webhook-Id", outboxId);
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Webhook-Timestamp", Instant.now().toString());
            headers.set("X-Webhook-Event", outbox.getEventType());
            headers.set("X-Webhook-Event-Version", outbox.getEventVersion());
            headers.set("X-Webhook-Delivery", outboxId);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint.getUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            // Check for redirect responses (3xx) and treat as errors
            if (response.getStatusCode().is3xxRedirection()) {
                String location = response.getHeaders() != null ? response.getHeaders().getFirst("Location") : "unknown";
                logger.error("Webhook delivery returned redirect status {} to location {}. Redirects are not allowed for SSRF protection.",
                        response.getStatusCode(), location);
                metrics.recordDeliveryFailure();
                return new DeliveryResult(false, response.getStatusCode().value(),
                        "Redirect not allowed: " + response.getStatusCode() + " to " + location, response.getBody());
            }

            // Record success metric and latency
            metrics.recordDeliverySuccess();
            if (outbox.getCreatedAt() != null) {
                Duration latency = Duration.between(outbox.getCreatedAt(), Instant.now());
                metrics.recordDeliveryLatency(latency);
            }

            return new DeliveryResult(true, response.getStatusCode().value(), null, response.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            logger.error("Webhook delivery failed with HTTP status: {}", e.getStatusCode(), e);
            metrics.recordDeliveryFailure();
            return new DeliveryResult(false, e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Webhook delivery failed", e);
            metrics.recordDeliveryFailure();
            return new DeliveryResult(false, null, e.getMessage(), null);
        }
    }

    private void validateDeliveryTarget(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            // Re-validate at connection time to prevent DNS rebinding
            if (!SSRFProtection.isSafeHost(host)) {
                throw new SecurityException("Webhook target resolved to blocked address: " + host);
            }

            // Additional validation: resolve and check the actual IP
            InetAddress address = InetAddress.getByName(host);
            if (!SSRFProtection.isSafeAddress(address)) {
                throw new SecurityException("Webhook target IP is blocked: " + address.getHostAddress());
            }

        } catch (Exception e) {
            throw new SecurityException("Failed to validate webhook delivery target: " + e.getMessage(), e);
        }
    }

    public record DeliveryResult(
            boolean success,
            Integer httpStatus,
            String errorMessage,
            String responseBody
    ) {}
}
