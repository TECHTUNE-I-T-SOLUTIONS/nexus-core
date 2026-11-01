package com.techtune.nexus;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.domain.JobStatus;
import com.techtune.nexus.job.persistence.JobRepository;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class JobRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nexus_core")
            .withUsername("nexus")
            .withPassword("nexus_password");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @AfterEach
    void tearDown() {
        jobRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveJob() {
        Tenant tenant = new Tenant("Test Tenant");
        Tenant savedTenant = tenantRepository.save(tenant);

        Job job = new Job(savedTenant, "email.send", JsonNodeFactory.instance.objectNode());
        Job saved = jobRepository.save(job);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(savedTenant.getId(), saved.getTenantId());
    }

    @Test
    void shouldFindJobsByTenant() {
        Tenant tenant = new Tenant("Test Tenant");
        Tenant savedTenant = tenantRepository.save(tenant);

        Job job1 = new Job(savedTenant, "email.send", JsonNodeFactory.instance.objectNode());
        Job job2 = new Job(savedTenant, "notification.send", JsonNodeFactory.instance.objectNode());
        jobRepository.save(job1);
        jobRepository.save(job2);

        List<Job> jobs = jobRepository.findByTenantId(savedTenant.getId());
        assertEquals(2, jobs.size());
    }

    @Test
    void shouldFindJobsByTenantAndStatus() {
        Tenant tenant = new Tenant("Test Tenant");
        Tenant savedTenant = tenantRepository.save(tenant);

        Job job1 = new Job(savedTenant, "email.send", JsonNodeFactory.instance.objectNode());
        job1.queue();
        jobRepository.save(job1);

        Job job2 = new Job(savedTenant, "notification.send", JsonNodeFactory.instance.objectNode());
        jobRepository.save(job2);

        List<Job> queuedJobs = jobRepository.findByTenantIdAndStatus(savedTenant.getId(), JobStatus.QUEUED);
        assertEquals(1, queuedJobs.size());
    }

    @Test
    void shouldFindJobsByStatus() {
        Tenant tenant = new Tenant("Test Tenant");
        Tenant savedTenant = tenantRepository.save(tenant);

        Job job1 = new Job(savedTenant, "email.send", JsonNodeFactory.instance.objectNode());
        job1.queue();
        jobRepository.save(job1);

        Job job2 = new Job(savedTenant, "notification.send", JsonNodeFactory.instance.objectNode());
        job2.queue();
        jobRepository.save(job2);

        List<Job> queuedJobs = jobRepository.findByStatus(JobStatus.QUEUED);
        assertEquals(2, queuedJobs.size());
    }
}
