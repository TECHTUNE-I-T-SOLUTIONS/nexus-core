package com.techtune.nexus.webhook.delivery;

import com.techtune.nexus.webhook.SSRFProtection;
import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import com.techtune.nexus.webhook.domain.WebhookOutbox;
import com.techtune.nexus.webhook.event.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class WebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;

    private final RestTemplate restTemplate;

    public WebhookDeliveryService() {
        this.restTemplate = createRestTemplate();
    }

    // Constructor for testing with mocked RestTemplate
    public WebhookDeliveryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT_SECONDS * 1000);
        factory.setReadTimeout(RESPONSE_TIMEOUT_SECONDS * 1000);
        return new RestTemplate(factory);
    }

    public DeliveryResult deliver(WebhookEndpoint endpoint, WebhookOutbox outbox) {
        try {
            // Validate the target URL at connection time (DNS rebinding protection)
            validateDeliveryTarget(endpoint.getUrl());

            String body = outbox.getPayload();
            WebhookEvent event = WebhookEvent.fromString(body);
            String signature = generateSignature(endpoint.getSecret(), body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Id", outbox.getId().toString());
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Webhook-Timestamp", Instant.now().toString());
            headers.set("X-Webhook-Event", event.eventType());
            headers.set("X-Webhook-Event-Version", event.eventVersion());
            headers.set("X-Webhook-Delivery", outbox.getId().toString());

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint.getUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return new DeliveryResult(true, response.getStatusCode().value(), null, response.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            logger.error("Webhook delivery failed with HTTP status: {}", e.getStatusCode(), e);
            return new DeliveryResult(false, e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Webhook delivery failed", e);
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

    public String generateSignature(String secret, String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String message = secret + payload;
            byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    public record DeliveryResult(
            boolean success,
            Integer httpStatus,
            String errorMessage,
            String responseBody
    ) {}
}
