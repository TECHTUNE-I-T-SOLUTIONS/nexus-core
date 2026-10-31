package com.techtune.nexus.schedule;

import com.techtune.nexus.job.application.JobService;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.schedule.api.CreateScheduleRequest;
import com.techtune.nexus.schedule.application.ScheduleService;
import com.techtune.nexus.schedule.domain.Schedule;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Background scheduler that processes due schedules and generates job instances.
 * Uses transactional claiming to prevent duplicate job generation.
 */
@Component
@ConditionalOnProperty(name = "nexus.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private final ScheduleService scheduleService;
    private final JobService jobService;
    private final TenantRepository tenantRepository;

    public Scheduler(ScheduleService scheduleService, JobService jobService, TenantRepository tenantRepository) {
        this.scheduleService = scheduleService;
        this.jobService = jobService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Poll for due schedules and generate job instances.
     * Runs at a fixed interval configured by nexus.scheduler.pollingInterval.
     */
    @Scheduled(fixedDelayString = "${nexus.scheduler.pollingInterval:60s}", initialDelayString = "10s")
    @Transactional
    public void processDueSchedules() {
        try {
            List<Schedule> dueSchedules = scheduleService.findDueSchedules();
            logger.debug("Found {} due schedules", dueSchedules.size());

            for (Schedule schedule : dueSchedules) {
                processSchedule(schedule);
            }
        } catch (Exception e) {
            logger.error("Error processing due schedules", e);
        }
    }

    private void processSchedule(Schedule schedule) {
        try {
            // Create a job instance from the schedule
            UUID tenantId = schedule.getTenantId();
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));

            JsonNode payload = schedule.getPayload();
            Job job = jobService.createJob(
                    tenantId,
                    schedule.getJobType(),
                    payload,
                    schedule.getJobPriority(),
                    schedule.getNextExecutionAt(),
                    schedule.getJobMaxAttempts(),
                    schedule.getJobTimeoutSeconds(),
                    null // No idempotency key for scheduled jobs
            );

            logger.info("Created job {} for schedule {}", job.getId(), schedule.getId());

            // Advance the schedule to the next execution time
            Instant nextExecution = CronEvaluator.calculateNextExecution(
                    schedule.getCronExpression(),
                    schedule.getTimezone(),
                    Instant.now()
            ).orElseThrow(() -> new IllegalStateException("Cron expression never matches"));

            schedule.setNextExecutionAt(nextExecution);
            scheduleService.updateSchedule(schedule.getId(), toCreateRequest(schedule));

            logger.info("Advanced schedule {} to next execution at {}", schedule.getId(), nextExecution);

        } catch (Exception e) {
            logger.error("Error processing schedule {}", schedule.getId(), e);
        }
    }

    private CreateScheduleRequest toCreateRequest(Schedule schedule) {
        return new CreateScheduleRequest(
                schedule.getTenantId(),
                schedule.getJobType(),
                schedule.getPayload(),
                schedule.getCronExpression(),
                schedule.getTimezone(),
                schedule.getJobPriority(),
                schedule.getJobMaxAttempts(),
                schedule.getJobTimeoutSeconds()
        );
    }
}
