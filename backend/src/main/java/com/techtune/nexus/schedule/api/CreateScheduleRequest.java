package com.techtune.nexus.schedule.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateScheduleRequest(
        @NotNull UUID tenantId,
        @NotBlank String jobType,
        @NotNull JsonNode payload,
        @NotBlank @Pattern(regexp = "^[0-9,*/\\- ]+$", message = "Invalid cron expression") String cronExpression,
        @NotBlank String timezone,
        Integer jobPriority,
        Integer jobMaxAttempts,
        Integer jobTimeoutSeconds
) {
    public CreateScheduleRequest {
        if (jobPriority != null && (jobPriority < 0 || jobPriority > 10)) {
            throw new IllegalArgumentException("Priority must be between 0 and 10");
        }
        if (jobMaxAttempts != null && jobMaxAttempts < 1) {
            throw new IllegalArgumentException("Max attempts must be at least 1");
        }
    }
}
