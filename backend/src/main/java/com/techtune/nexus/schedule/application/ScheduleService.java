package com.techtune.nexus.schedule.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.techtune.nexus.schedule.CronEvaluator;
import com.techtune.nexus.schedule.api.CreateScheduleRequest;
import com.techtune.nexus.schedule.domain.Schedule;
import com.techtune.nexus.schedule.persistence.ScheduleRepository;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TenantRepository tenantRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, TenantRepository tenantRepository) {
        this.scheduleRepository = scheduleRepository;
        this.tenantRepository = tenantRepository;
    }

    private UUID getAuthenticatedTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UUID) {
                return (UUID) principal;
            }
        }
        return null;
    }

    public Schedule createSchedule(CreateScheduleRequest request) {
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(request.tenantId())) {
            throw new IllegalArgumentException("Cannot create schedules for other tenants");
        }
        
        // Validate cron expression
        CronEvaluator.validate(request.cronExpression());
        
        // Validate timezone
        try {
            java.time.ZoneId.of(request.timezone());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + request.timezone());
        }

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.tenantId()));

        Schedule schedule = new Schedule(tenant, request.jobType(), request.payload(), request.cronExpression(), request.timezone());
        
        if (request.jobPriority() != null) {
            schedule.setJobPriority(request.jobPriority());
        }
        if (request.jobMaxAttempts() != null) {
            schedule.setJobMaxAttempts(request.jobMaxAttempts());
        }
        if (request.jobTimeoutSeconds() != null) {
            schedule.setJobTimeoutSeconds(request.jobTimeoutSeconds());
        }

        // Calculate initial next execution time
        Instant nextExecution = CronEvaluator.calculateNextExecution(request.cronExpression(), request.timezone(), Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("Cron expression never matches"));
        schedule.setNextExecutionAt(nextExecution);

        return scheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public Optional<Schedule> getSchedule(UUID id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElse(null);
        
        if (schedule != null) {
            UUID authenticatedTenantId = getAuthenticatedTenantId();
            if (authenticatedTenantId != null && !authenticatedTenantId.equals(schedule.getTenantId())) {
                throw new IllegalArgumentException("Cannot access schedules from other tenants");
            }
        }
        
        return Optional.ofNullable(schedule);
    }

    @Transactional(readOnly = true)
    public List<Schedule> listSchedules(UUID tenantId) {
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null) {
            tenantId = authenticatedTenantId;
        }
        
        if (tenantId != null) {
            return scheduleRepository.findByTenantId(tenantId);
        }
        return scheduleRepository.findAll();
    }

    public Schedule updateSchedule(UUID id, CreateScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(schedule.getTenantId())) {
            throw new IllegalArgumentException("Cannot modify schedules from other tenants");
        }

        // Validate cron expression if changed
        if (!schedule.getCronExpression().equals(request.cronExpression())) {
            CronEvaluator.validate(request.cronExpression());
            schedule.setCronExpression(request.cronExpression());
        }

        // Validate timezone if changed
        if (!schedule.getTimezone().equals(request.timezone())) {
            try {
                java.time.ZoneId.of(request.timezone());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid timezone: " + request.timezone());
            }
            schedule.setTimezone(request.timezone());
        }

        schedule.setJobType(request.jobType());
        schedule.setPayload(request.payload());

        if (request.jobPriority() != null) {
            schedule.setJobPriority(request.jobPriority());
        }
        if (request.jobMaxAttempts() != null) {
            schedule.setJobMaxAttempts(request.jobMaxAttempts());
        }
        if (request.jobTimeoutSeconds() != null) {
            schedule.setJobTimeoutSeconds(request.jobTimeoutSeconds());
        }

        // Recalculate next execution time
        Instant nextExecution = CronEvaluator.calculateNextExecution(schedule.getCronExpression(), schedule.getTimezone(), Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("Cron expression never matches"));
        schedule.setNextExecutionAt(nextExecution);

        return scheduleRepository.save(schedule);
    }

    public Schedule pauseSchedule(UUID id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(schedule.getTenantId())) {
            throw new IllegalArgumentException("Cannot modify schedules from other tenants");
        }
        
        schedule.setEnabled(false);
        return scheduleRepository.save(schedule);
    }

    public Schedule resumeSchedule(UUID id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(schedule.getTenantId())) {
            throw new IllegalArgumentException("Cannot modify schedules from other tenants");
        }
        
        schedule.setEnabled(true);
        
        // Recalculate next execution time
        Instant nextExecution = CronEvaluator.calculateNextExecution(schedule.getCronExpression(), schedule.getTimezone(), Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("Cron expression never matches"));
        schedule.setNextExecutionAt(nextExecution);
        
        return scheduleRepository.save(schedule);
    }

    public void deleteSchedule(UUID id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID authenticatedTenantId = getAuthenticatedTenantId();
        if (authenticatedTenantId != null && !authenticatedTenantId.equals(schedule.getTenantId())) {
            throw new IllegalArgumentException("Cannot delete schedules from other tenants");
        }
        
        scheduleRepository.delete(schedule);
    }

    @Transactional(readOnly = true)
    public List<Schedule> findDueSchedules() {
        return scheduleRepository.findDueSchedules(Instant.now());
    }
}
