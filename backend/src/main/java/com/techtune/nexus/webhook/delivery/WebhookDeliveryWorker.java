package com.techtune.nexus.webhook.delivery;

import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import com.techtune.nexus.webhook.domain.WebhookOutbox;
import com.techtune.nexus.webhook.persistence.WebhookEndpointRepository;
import com.techtune.nexus.webhook.persistence.WebhookOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "nexus.webhook.delivery.enabled", havingValue = "true", matchIfMissing = true)
public class WebhookDeliveryWorker {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryWorker.class);
    private static final int BATCH_SIZE = 10;
    private static final int LEASE_DURATION_SECONDS = 300;

    private final WebhookOutboxRepository webhookOutboxRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryService deliveryService;

    public WebhookDeliveryWorker(
            WebhookOutboxRepository webhookOutboxRepository,
            WebhookEndpointRepository webhookEndpointRepository,
            WebhookDeliveryService deliveryService) {
        this.webhookOutboxRepository = webhookOutboxRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelayString = "${nexus.webhook.polling-interval:30000}")
    @Transactional
    public void processPendingWebhooks() {
        Instant now = Instant.now();
        List<WebhookOutbox> pendingWebhooks = webhookOutboxRepository.findClaimableWebhooks(now, BATCH_SIZE);

        for (WebhookOutbox outbox : pendingWebhooks) {
            try {
                // Claim the webhook with a lease
                outbox.setLeaseExpiresAt(now.plusSeconds(LEASE_DURATION_SECONDS));
                webhookOutboxRepository.save(outbox);

                // Find the webhook endpoint
                List<WebhookEndpoint> endpoints = webhookEndpointRepository.findEnabledByTenantIdAndEventType(
                        outbox.getTenant().getId(),
                        outbox.getEventType()
                );

                if (endpoints.isEmpty()) {
                    logger.warn("No enabled webhook endpoints found for tenant {} and event type {}",
                            outbox.getTenant().getId(), outbox.getEventType());
                    outbox.setErrorMessage("No enabled webhook endpoints");
                    outbox.incrementRetryCount();
                    outbox.scheduleRetry(calculateNextRetry(outbox.getRetryCount()));
                    webhookOutboxRepository.save(outbox);
                    continue;
                }

                // Deliver to each endpoint
                for (WebhookEndpoint endpoint : endpoints) {
                    deliveryService.deliver(endpoint, outbox);
                }

                // Mark as delivered
                outbox.markDelivered();
                webhookOutboxRepository.save(outbox);

                logger.info("Webhook {} delivered successfully", outbox.getId());

            } catch (Exception e) {
                logger.error("Failed to process webhook {}", outbox.getId(), e);
                outbox.incrementRetryCount();
                outbox.setErrorMessage(e.getMessage());
                
                if (outbox.isRetryable()) {
                    outbox.scheduleRetry(calculateNextRetry(outbox.getRetryCount()));
                } else {
                    outbox.setErrorMessage("Max retries exceeded: " + e.getMessage());
                }
                webhookOutboxRepository.save(outbox);
            }
        }
    }

    private Instant calculateNextRetry(int retryCount) {
        // Exponential backoff with jitter: min(30s * 2^retryCount, 3600s) + random jitter
        long baseDelaySeconds = 30L;
        long maxDelaySeconds = 3600L;
        long delaySeconds = Math.min(baseDelaySeconds * (1L << retryCount), maxDelaySeconds);
        long jitter = (long) (Math.random() * delaySeconds * 0.1); // 10% jitter
        return Instant.now().plusSeconds(delaySeconds + jitter);
    }
}
