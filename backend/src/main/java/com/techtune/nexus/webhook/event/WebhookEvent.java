package com.techtune.nexus.webhook.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.UUID;

public record WebhookEvent(
        String id,
        String eventType,
        String eventVersion,
        Instant occurredAt,
        UUID tenantId,
        UUID jobId,
        String jobType,
        String jobStatus,
        String payload
) {
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

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

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize WebhookEvent to JSON", e);
        }
    }

    public static WebhookEvent fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, WebhookEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize WebhookEvent from JSON", e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
