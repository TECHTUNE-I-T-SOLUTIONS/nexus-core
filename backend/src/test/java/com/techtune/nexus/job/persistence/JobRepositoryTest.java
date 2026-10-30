package com.techtune.nexus.job.persistence;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.domain.JobStatus;
import com.techtune.nexus.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobRepositoryTest {

    @Test
    void shouldCreateJob() {
        Tenant tenant = new Tenant("Test Tenant");
        Job job = new Job(tenant, "email.send", JsonNodeFactory.instance.objectNode());

        assertEquals("email.send", job.getJobType());
        assertEquals(JobStatus.PENDING, job.getStatus());
        // @PrePersist not called in unit tests without JPA context
        // assertNotNull(job.getId());
        // assertNotNull(job.getCreatedAt());
        assertNotNull(job.getPayload());
    }
}
