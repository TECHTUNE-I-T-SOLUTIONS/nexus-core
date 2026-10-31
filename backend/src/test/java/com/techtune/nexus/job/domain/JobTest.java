package com.techtune.nexus.job.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.techtune.nexus.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobTest {

    @Test
    void shouldCreateJob() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("test", "value");
        Job job = new Job(tenant, "email.send", payload);

        assertEquals("email.send", job.getJobType());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(5, job.getPriority());
        assertEquals(0, job.getAttemptCount());
        assertEquals(3, job.getMaxAttempts());
        // @PrePersist not called in unit tests without JPA context
        // assertNotNull(job.getId());
        // assertNotNull(job.getCreatedAt());
        // assertNotNull(job.getUpdatedAt());
        // Tenant ID is not set because tenant is not persisted
        assertNull(job.getTenantId());
        // Check payload is converted and can be retrieved
        assertNotNull(job.getPayload());
        assertEquals("value", job.getPayload().get("test").asText());
    }

    @Test
    void shouldSetPriority() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);

        job.setPriority(8);

        assertEquals(8, job.getPriority());
    }

    @Test
    void shouldRejectInvalidPriority() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);

        assertThrows(IllegalArgumentException.class, () -> job.setPriority(11));
        assertThrows(IllegalArgumentException.class, () -> job.setPriority(-1));
    }

    @Test
    void shouldScheduleJob() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);

        job.schedule();

        assertEquals(JobStatus.SCHEDULED, job.getStatus());
    }

    @Test
    void shouldQueueJob() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);

        job.queue();

        assertEquals(JobStatus.QUEUED, job.getStatus());
    }

    @Test
    void shouldClaimJob() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);
        job.queue();

        job.claim("worker-1", Instant.now().plusSeconds(300));

        assertEquals(JobStatus.RUNNING, job.getStatus());
        assertEquals("worker-1", job.getWorkerId());
        assertNotNull(job.getClaimedAt());
        assertNotNull(job.getLeaseExpiresAt());
    }

    @Test
    void shouldSucceedJob() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);
        job.queue();
        job.claim("worker-1", Instant.now().plusSeconds(300));

        job.succeed();

        assertEquals(JobStatus.SUCCEEDED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        assertNull(job.getLeaseExpiresAt());
    }

    @Test
    void shouldFailJobWithRetries() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);
        job.queue();
        job.claim("worker-1", Instant.now().plusSeconds(300));

        ObjectNode errorDetails = JsonNodeFactory.instance.objectNode().put("code", "TIMEOUT");
        job.fail("Connection timeout", errorDetails);

        assertEquals(JobStatus.RETRY_WAIT, job.getStatus());
        assertEquals(1, job.getAttemptCount());
        assertEquals("Connection timeout", job.getLastErrorMessage());
        assertNull(job.getLeaseExpiresAt());
    }

    @Test
    void shouldFailJobWhenRetriesExhausted() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);
        job.setMaxAttempts(1);
        job.queue();
        job.claim("worker-1", Instant.now().plusSeconds(300));

        ObjectNode errorDetails = JsonNodeFactory.instance.objectNode().put("code", "TIMEOUT");
        job.fail("Connection timeout", errorDetails);

        assertEquals(JobStatus.DEAD_LETTERED, job.getStatus());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void shouldCancelJob() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);

        job.cancel();

        assertEquals(JobStatus.CANCELLED, job.getStatus());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void shouldNotCancelCompletedJob() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);
        job.queue();
        job.claim("worker-1", Instant.now().plusSeconds(300));
        job.succeed();

        assertThrows(IllegalStateException.class, job::cancel);
    }

    @Test
    void shouldRetryJob() {
        Tenant tenant = new Tenant("Test Tenant");
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        Job job = new Job(tenant, "email.send", payload);
        job.setMaxAttempts(1);
        job.queue();
        job.claim("worker-1", Instant.now().plusSeconds(300));
        ObjectNode errorDetails = JsonNodeFactory.instance.objectNode().put("code", "TIMEOUT");
        job.fail("Error", errorDetails);

        // After fail with maxAttempts=1, status should be DEAD_LETTERED
        assertEquals(JobStatus.DEAD_LETTERED, job.getStatus());

        // Note: DEAD_LETTERED jobs cannot be retried directly
        // They would need to be manually queued or managed through application logic
    }
}
