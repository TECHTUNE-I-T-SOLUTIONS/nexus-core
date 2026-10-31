package com.techtune.nexus.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.techtune.nexus.job.application.JobService;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.domain.JobStatus;
import com.techtune.nexus.job.persistence.JobRepository;
import com.techtune.nexus.tenant.application.TenantService;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import com.techtune.nexus.worker.engine.JobClaimingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class JobClaimingConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    );

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JobClaimingService claimingService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private JobService jobService;

    @AfterEach
    void tearDown() {
        jobRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    void shouldPreventDuplicateClaimingWithConcurrentWorkers() throws InterruptedException {
        // Create tenant with unique name
        String tenantName = "Test Tenant " + UUID.randomUUID();
        Tenant tenant = tenantService.createTenant(tenantName);

        // Create 10 jobs
        int jobCount = 10;
        ObjectNode payload = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < jobCount; i++) {
            jobService.createJob(tenant.getId(), "test-job", payload, 5, null, 3, 60, null);
        }

        // Wait for jobs to be queued
        Thread.sleep(100);

        // Simulate 3 workers claiming concurrently
        int workerCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch latch = new CountDownLatch(workerCount);
        AtomicInteger totalClaimed = new AtomicInteger(0);

        for (int i = 0; i < workerCount; i++) {
            final String workerId = "worker-" + i;
            executor.submit(() -> {
                try {
                    List<Job> claimed = claimingService.claimJobs(
                        tenant.getId(), 
                        workerId, 
                        10, 
                        60
                    );
                    totalClaimed.addAndGet(claimed.size());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Total claimed should equal job count (no duplicates)
        assertEquals(jobCount, totalClaimed.get());

        // Verify each job was claimed exactly once
        List<Job> allJobs = jobRepository.findByTenantId(tenant.getId());
        long claimedCount = allJobs.stream()
            .filter(j -> j.getStatus() == JobStatus.RUNNING)
            .count();
        assertEquals(jobCount, claimedCount);
    }

    @Test
    void shouldRespectBatchSizeInDatabase() {
        // Create tenant with unique name
        String tenantName = "Test Tenant " + UUID.randomUUID();
        Tenant tenant = tenantService.createTenant(tenantName);

        // Create 20 jobs
        int jobCount = 20;
        ObjectNode payload = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < jobCount; i++) {
            jobService.createJob(tenant.getId(), "test-job", payload, 5, null, 3, 60, null);
        }

        // Wait for jobs to be queued
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Claim with batch size of 5
        List<Job> claimed = claimingService.claimJobs(tenant.getId(), "worker-1", 5, 60);

        // Should only claim 5 jobs
        assertEquals(5, claimed.size());
    }

    @Test
    void shouldRespectTenantIsolation() {
        // Create two tenants with unique names
        String tenantName1 = "Tenant 1 " + UUID.randomUUID();
        String tenantName2 = "Tenant 2 " + UUID.randomUUID();
        Tenant tenant1 = tenantService.createTenant(tenantName1);
        Tenant tenant2 = tenantService.createTenant(tenantName2);

        // Create jobs for tenant 1
        ObjectNode payload = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < 5; i++) {
            jobService.createJob(tenant1.getId(), "test-job", payload, 5, null, 3, 60, null);
        }

        // Create jobs for tenant 2
        for (int i = 0; i < 5; i++) {
            jobService.createJob(tenant2.getId(), "test-job", payload, 5, null, 3, 60, null);
        }

        // Wait for jobs to be queued
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Claim jobs for tenant 1
        List<Job> claimed = claimingService.claimJobs(tenant1.getId(), "worker-1", 10, 60);

        // Should only claim tenant 1's jobs
        assertEquals(5, claimed.size());
        assertTrue(claimed.stream().allMatch(j -> j.getTenant().getId().equals(tenant1.getId())));
    }
}
