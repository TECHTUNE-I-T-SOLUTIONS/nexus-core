package com.techtune.nexus.schedule.api;

import com.techtune.nexus.schedule.domain.Schedule;

import java.time.Instant;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        UUID tenantId,
        String jobType,
        String cronExpression,
        String timezone,
        Instant nextExecutionAt,
        Boolean enabled,
        Integer jobPriority,
        Integer jobMaxAttempts,
        Integer jobTimeoutSeconds,
        Instant createdAt,
        Instant updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTenantId(),
                schedule.getJobType(),
                schedule.getCronExpression(),
                schedule.getTimezone(),
                schedule.getNextExecutionAt(),
                schedule.isEnabled(),
                schedule.getJobPriority(),
                schedule.getJobMaxAttempts(),
                schedule.getJobTimeoutSeconds(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
