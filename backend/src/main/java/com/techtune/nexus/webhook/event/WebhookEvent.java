package com.techtune.nexus.webhook.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record WebhookEvent(
        String id,
        String eventType,
        String eventVersion,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        Instant occurredAt,
        UUID tenantId,
        UUID jobId,
        String jobType,
        String jobStatus,
        String payload
) {
    public static final String EVENT_VERSION = "1.0";
    public static final String EVENT_JOB_COMPLETED = "job.completed";
    public static final String EVENT_JOB_FAILED = "job.failed";
    public static final String EVENT_JOB_CANCELLED = "job.cancelled";

    public static WebhookEvent jobCompleted(UUID tenantId, UUID jobId, String jobType, String payload) {
        return new WebhookEvent(
                UUID.randomUUID().toString(),
                EVENT_JOB_COMPLETED,
                EVENT_VERSION,
                Instant.now(),
                tenantId,
                jobId,
                jobType,
                "COMPLETED",
                payload
        );
    }

    public static WebhookEvent jobFailed(UUID tenantId, UUID jobId, String jobType, String errorMessage, String payload) {
        return new WebhookEvent(
                UUID.randomUUID().toString(),
                EVENT_JOB_FAILED,
                EVENT_VERSION,
                Instant.now(),
                tenantId,
                jobId,
                jobType,
                "FAILED",
                payload
        );
    }

    public static WebhookEvent jobCancelled(UUID tenantId, UUID jobId, String jobType, String payload) {
        return new WebhookEvent(
                UUID.randomUUID().toString(),
                EVENT_JOB_CANCELLED,
                EVENT_VERSION,
                Instant.now(),
                tenantId,
                jobId,
                jobType,
                "CANCELLED",
                payload
        );
    }
}
