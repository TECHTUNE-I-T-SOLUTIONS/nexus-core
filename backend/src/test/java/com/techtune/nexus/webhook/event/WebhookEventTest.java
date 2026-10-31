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
}
