package com.techtune.nexus.schedule;

import com.techtune.nexus.job.application.JobService;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.persistence.JobRepository;
import com.techtune.nexus.schedule.application.ScheduleService;
import com.techtune.nexus.schedule.domain.Schedule;
import com.techtune.nexus.schedule.persistence.ScheduleRepository;
import com.techtune.nexus.tenant.application.TenantService;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class SchedulerConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    );

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private com.techtune.nexus.tenant.persistence.TenantRepository tenantRepository;

    @AfterEach
    void cleanup() {
        jobRepository.deleteAll();
        scheduleRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    void schedulerDuplicatePrevention() throws InterruptedException {
        // Given
        Tenant tenant = tenantService.createTenant("Scheduler Test Tenant " + UUID.randomUUID());
        
        var request = new com.techtune.nexus.schedule.api.CreateScheduleRequest(
                tenant.getId(),
                "test.job",
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
                "0 * * * *",
                "UTC",
                5,
                3,
                null
        );
        
        Schedule schedule = scheduleService.createSchedule(request);
        Instant scheduledTime = schedule.getNextExecutionAt();
        
        // Create first job - should succeed
        Job job1 = jobService.createJob(
                tenant.getId(),
                schedule.getJobType(),
                schedule.getPayload(),
                schedule.getJobPriority(),
                scheduledTime,
                schedule.getJobMaxAttempts(),
                schedule.getJobTimeoutSeconds(),
                null,
                schedule.getId()
        );
        
        assertNotNull(job1);
        
        // Try to create second job with same schedule_id and scheduled_at - should fail
        assertThrows(Exception.class, () -> {
            jobService.createJob(
                    tenant.getId(),
                    schedule.getJobType(),
                    schedule.getPayload(),
                    schedule.getJobPriority(),
                    scheduledTime,
                    schedule.getJobMaxAttempts(),
                    schedule.getJobTimeoutSeconds(),
                    null,
                    schedule.getId()
            );
        }, "Should fail when creating duplicate job for same schedule and time");
        
        // Verify only one job exists
        List<Job> jobs = jobRepository.findByScheduleId(schedule.getId());
        assertEquals(1, jobs.size(), "Only one job should exist for the schedule");
    }
}
