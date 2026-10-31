package com.techtune.nexus.webhook.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Metrics for webhook delivery operations.
 * 
 * Tracks:
 * - Successful deliveries
 * - Failed delivery attempts
 * - Scheduled retries
 * - Dead-letter transitions
 * - Pending deliveries
 * - Delivery latency
 * - Expired lease recovery
 */
@Component
public class WebhookDeliveryMetrics {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryMetrics.class);

    private final Counter deliveriesSuccessful;
    private final Counter deliveriesFailed;
    private final Counter deliveriesRetried;
    private final Counter deliveriesDeadLettered;
    private final Counter leasesRecovered;
    private final Timer deliveryLatency;

    public WebhookDeliveryMetrics(MeterRegistry registry) {
        this.deliveriesSuccessful = Counter.builder("webhook.deliveries.successful")
                .description("Number of successful webhook deliveries")
                .register(registry);

        this.deliveriesFailed = Counter.builder("webhook.deliveries.failed")
                .description("Number of failed webhook delivery attempts")
                .register(registry);

        this.deliveriesRetried = Counter.builder("webhook.deliveries.retried")
                .description("Number of webhook deliveries scheduled for retry")
                .register(registry);

        this.deliveriesDeadLettered = Counter.builder("webhook.deliveries.dead_lettered")
                .description("Number of webhook deliveries moved to dead-letter queue")
                .register(registry);

        this.leasesRecovered = Counter.builder("webhook.leases.recovered")
                .description("Number of expired delivery leases recovered")
                .register(registry);

        this.deliveryLatency = Timer.builder("webhook.delivery.latency")
                .description("Time from event creation to successful delivery")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);

        logger.info("Webhook delivery metrics registered");
    }

    public void recordDeliverySuccess() {
        deliveriesSuccessful.increment();
    }

    public void recordDeliveryFailure() {
        deliveriesFailed.increment();
    }

    public void recordDeliveryRetry() {
        deliveriesRetried.increment();
    }

    public void recordDeadLetterTransition() {
        deliveriesDeadLettered.increment();
    }

    public void recordLeaseRecovery() {
        leasesRecovered.increment();
    }

    public void recordDeliveryLatency(Duration latency) {
        deliveryLatency.record(latency);
    }
}
