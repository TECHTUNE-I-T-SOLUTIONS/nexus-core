package com.techtune.nexus.job.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.techtune.nexus.job.domain.Job;

import java.time.Instant;
import java.util.UUID;

public class JobResponse {

    private UUID id;
    private UUID tenantId;
    private String jobType;
    private JsonNode payload;
    private Integer priority;
    private String status;
    private Instant scheduledAt;
    private Integer attemptCount;
    private Integer maxAttempts;
    private String workerId;
    private Instant claimedAt;
    private Instant leaseExpiresAt;
    private Instant lastExecutionStartedAt;
    private Instant lastExecutionCompletedAt;
    private String lastErrorMessage;
    private JsonNode lastErrorDetails;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static JobResponse from(Job job) {
        JobResponse response = new JobResponse();
        response.id = job.getId();
        response.tenantId = job.getTenantId();
        response.jobType = job.getJobType();
        response.payload = job.getPayload();
        response.priority = job.getPriority();
        response.status = job.getStatus().name();
        response.scheduledAt = job.getScheduledAt();
        response.attemptCount = job.getAttemptCount();
        response.maxAttempts = job.getMaxAttempts();
        response.workerId = job.getWorkerId();
        response.claimedAt = job.getClaimedAt();
        response.leaseExpiresAt = job.getLeaseExpiresAt();
        response.lastExecutionStartedAt = job.getLastExecutionStartedAt();
        response.lastExecutionCompletedAt = job.getLastExecutionCompletedAt();
        response.lastErrorMessage = job.getLastErrorMessage();
        response.lastErrorDetails = job.getLastErrorDetails();
        response.completedAt = job.getCompletedAt();
        response.createdAt = job.getCreatedAt();
        response.updatedAt = job.getUpdatedAt();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getJobType() {
        return jobType;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public String getWorkerId() {
        return workerId;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public Instant getLastExecutionStartedAt() {
        return lastExecutionStartedAt;
    }

    public Instant getLastExecutionCompletedAt() {
        return lastExecutionCompletedAt;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public JsonNode getLastErrorDetails() {
        return lastErrorDetails;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
