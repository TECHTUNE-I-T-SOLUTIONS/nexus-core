package com.techtune.nexus.webhook.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WebhookEventTest {

    @Test
    void testJobCompletedEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        String jobType = "test-job";
        String payload = "{\"status\":\"success\"}";

        WebhookEvent event = WebhookEvent.jobCompleted(tenantId, jobId, jobType, payload);

        assertNotNull(event);
        assertNotNull(event.id());
        assertEquals(WebhookEvent.EVENT_JOB_COMPLETED, event.eventType());
        assertEquals(WebhookEvent.EVENT_VERSION, event.eventVersion());
        assertNotNull(event.occurredAt());
        assertEquals(tenantId, event.tenantId());
        assertEquals(jobId, event.jobId());
        assertEquals(jobType, event.jobType());
        assertEquals("COMPLETED", event.jobStatus());
        assertEquals(payload, event.payload());
    }

    @Test
    void testJobFailedEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        String jobType = "test-job";
        String errorMessage = "Execution failed";
        String payload = "{\"status\":\"failed\"}";

        WebhookEvent event = WebhookEvent.jobFailed(tenantId, jobId, jobType, errorMessage, payload);

        assertNotNull(event);
        assertEquals(WebhookEvent.EVENT_JOB_FAILED, event.eventType());
        assertEquals(WebhookEvent.EVENT_VERSION, event.eventVersion());
        assertEquals(tenantId, event.tenantId());
        assertEquals(jobId, event.jobId());
        assertEquals(jobType, event.jobType());
        assertEquals("FAILED", event.jobStatus());
        assertEquals(payload, event.payload());
    }

    @Test
    void testJobCancelledEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        String jobType = "test-job";
        String payload = "{\"status\":\"cancelled\"}";

        WebhookEvent event = WebhookEvent.jobCancelled(tenantId, jobId, jobType, payload);

        assertNotNull(event);
        assertEquals(WebhookEvent.EVENT_JOB_CANCELLED, event.eventType());
        assertEquals(WebhookEvent.EVENT_VERSION, event.eventVersion());
        assertEquals(tenantId, event.tenantId());
        assertEquals(jobId, event.jobId());
        assertEquals(jobType, event.jobType());
        assertEquals("CANCELLED", event.jobStatus());
        assertEquals(payload, event.payload());
    }

    @Test
    void testEventIdIsUnique() {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        WebhookEvent event1 = WebhookEvent.jobCompleted(tenantId, jobId, "test-job", "{}");
        WebhookEvent event2 = WebhookEvent.jobCompleted(tenantId, jobId, "test-job", "{}");

        assertNotEquals(event1.id(), event2.id());
    }

    @Test
    void testEventVersionConstant() {
        assertEquals("1.0", WebhookEvent.EVENT_VERSION);
    }

    @Test
    void testEventTypeConstants() {
        assertEquals("job.completed", WebhookEvent.EVENT_JOB_COMPLETED);
        assertEquals("job.failed", WebhookEvent.EVENT_JOB_FAILED);
        assertEquals("job.cancelled", WebhookEvent.EVENT_JOB_CANCELLED);
    }

    @Test
    void testToJsonSerialization() {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        String jobType = "test-job";
        String payload = "{\"status\":\"success\"}";

        WebhookEvent event = WebhookEvent.jobCompleted(tenantId, jobId, jobType, payload);
        String json = event.toJson();

        assertNotNull(json);
        assertTrue(json.contains("\"eventType\":\"job.completed\""));
        assertTrue(json.contains("\"eventVersion\":\"1.0\""));
        assertTrue(json.contains("\"jobType\":\"test-job\""));
        assertTrue(json.contains("\"jobStatus\":\"COMPLETED\""));
        // Payload is a string, so it's escaped in JSON
        assertTrue(json.contains("\"payload\""));
    }

    @Test
    void testFromJsonDeserialization() {
        String json = "{\"id\":\"test-id\",\"eventType\":\"job.completed\",\"eventVersion\":\"1.0\",\"occurredAt\":\"2024-01-01T00:00:00Z\",\"tenantId\":\"550e8400-e29b-41d4-a716-446655440000\",\"jobId\":\"660e8400-e29b-41d4-a716-446655440000\",\"jobType\":\"test-job\",\"jobStatus\":\"COMPLETED\",\"payload\":\"{\\\"status\\\":\\\"success\\\"}\"}";

        WebhookEvent event = WebhookEvent.fromJson(json);

        assertNotNull(event);
        assertEquals("test-id", event.id());
        assertEquals("job.completed", event.eventType());
        assertEquals("1.0", event.eventVersion());
        assertEquals("test-job", event.jobType());
        assertEquals("COMPLETED", event.jobStatus());
    }

    @Test
    void testJsonRoundTrip() {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        String jobType = "test-job";
        String payload = "{\"status\":\"success\"}";

        WebhookEvent original = WebhookEvent.jobCompleted(tenantId, jobId, jobType, payload);
        String json = original.toJson();
        WebhookEvent deserialized = WebhookEvent.fromJson(json);

        assertEquals(original.eventType(), deserialized.eventType());
        assertEquals(original.eventVersion(), deserialized.eventVersion());
        assertEquals(original.jobType(), deserialized.jobType());
        assertEquals(original.jobStatus(), deserialized.jobStatus());
        assertEquals(original.payload(), deserialized.payload());
    }

    @Test
    void testJsonEscapedPayload() {
        String payload = "{\"key\":\"value\"}";
        WebhookEvent event = WebhookEvent.jobCompleted(UUID.randomUUID(), UUID.randomUUID(), "test-job", payload);
        String json = event.toJson();

        assertNotNull(json);
        assertTrue(json.contains("\"payload\""));
        // Payload is a string field, so it should be quoted in JSON
        assertTrue(json.contains("\"payload\":\"{\\\"key\\\":\\\"value\\\"}\""));
    }
}
