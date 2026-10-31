package com.techtune.nexus.webhook.delivery;

import com.techtune.nexus.webhook.application.WebhookEndpointService;
import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import com.techtune.nexus.webhook.domain.WebhookOutbox;
import com.techtune.nexus.webhook.metrics.WebhookDeliveryMetrics;
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
    private final WebhookEndpointService webhookEndpointService;
    private final WebhookDeliveryService deliveryService;
    private final WebhookDeliveryMetrics metrics;

    public WebhookDeliveryWorker(
            WebhookOutboxRepository webhookOutboxRepository,
            WebhookEndpointService webhookEndpointService,
            WebhookDeliveryService deliveryService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) WebhookDeliveryMetrics metrics) {
        this.webhookOutboxRepository = webhookOutboxRepository;
        this.webhookEndpointService = webhookEndpointService;
        this.deliveryService = deliveryService;
        this.metrics = metrics != null ? metrics : new NoOpMetrics();
    }

    // No-op metrics implementation
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

    @Scheduled(fixedDelayString = "${nexus.webhook.delivery.polling-interval:30000}")
    @Transactional
    public void processPendingWebhooks() {
        Instant now = Instant.now();
        List<WebhookOutbox> pendingWebhooks = webhookOutboxRepository.findClaimableWebhooks(now);

        for (WebhookOutbox outbox : pendingWebhooks) {
            try {
                // Claim the webhook atomically
                int claimed = webhookOutboxRepository.claimWebhook(
                        outbox.getId(),
                        now.plusSeconds(LEASE_DURATION_SECONDS),
                        now
                );
                
                if (claimed == 0) {
                    // Another worker claimed it - this was likely a lease recovery
                    metrics.recordLeaseRecovery();
                    continue;
                }

                // Find the webhook endpoint
                List<WebhookEndpoint> endpoints = webhookEndpointService.findEnabledEndpointsForEvent(
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
                    metrics.recordDeliveryRetry();
                } else {
                    outbox.setErrorMessage("Max retries exceeded: " + e.getMessage());
                    metrics.recordDeadLetterTransition();
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
