package com.techtune.nexus.webhook.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.webhook.domain.WebhookOutbox;
import com.techtune.nexus.webhook.persistence.WebhookOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class WebhookEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventPublisher.class);

    private final WebhookOutboxRepository webhookOutboxRepository;

    public WebhookEventPublisher(WebhookOutboxRepository webhookOutboxRepository) {
        this.webhookOutboxRepository = webhookOutboxRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishJobCompleted(Job job) {
        try {
            String payload = jsonNodeToString(job.getPayload());
            WebhookEvent event = WebhookEvent.jobCompleted(
                    job.getTenant().getId(),
                    job.getId(),
                    job.getJobType(),
                    payload
            );
            createOutboxEntry(job, event);
        } catch (Exception e) {
            logger.error("Failed to publish job completed event for job {}", job.getId(), e);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishJobFailed(Job job, String errorMessage) {
        try {
            String payload = jsonNodeToString(job.getPayload());
            WebhookEvent event = WebhookEvent.jobFailed(
                    job.getTenant().getId(),
                    job.getId(),
                    job.getJobType(),
                    errorMessage,
                    payload
            );
            createOutboxEntry(job, event);
        } catch (Exception e) {
            logger.error("Failed to publish job failed event for job {}", job.getId(), e);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishJobCancelled(Job job) {
        try {
            String payload = jsonNodeToString(job.getPayload());
            WebhookEvent event = WebhookEvent.jobCancelled(
                    job.getTenant().getId(),
                    job.getId(),
                    job.getJobType(),
                    payload
            );
            createOutboxEntry(job, event);
        } catch (Exception e) {
            logger.error("Failed to publish job cancelled event for job {}", job.getId(), e);
        }
    }

    private void createOutboxEntry(Job job, WebhookEvent event) throws Exception {
        String payload = event.toJson();

        // Create outbox entry - the delivery worker will match to endpoints
        WebhookOutbox outbox = new WebhookOutbox(
                job.getTenant(),
                job,
                "https://placeholder", // Will be replaced by delivery worker
                payload
        );
        outbox.setEventType(event.eventType());
        outbox.setEventVersion(event.eventVersion());
        outbox.scheduleRetry(Instant.now());

        webhookOutboxRepository.save(outbox);
        logger.info("Created webhook outbox entry {} for job {} with event type {}",
                outbox.getId(), job.getId(), event.eventType());
    }

    private String jsonNodeToString(JsonNode jsonNode) {
        if (jsonNode == null) {
            return "{}";
        }
        return jsonNode.toString();
    }
}
