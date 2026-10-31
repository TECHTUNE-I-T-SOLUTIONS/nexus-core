package com.techtune.nexus.job.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techtune.nexus.common.domain.UuidIdentity;
import com.techtune.nexus.tenant.domain.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job extends UuidIdentity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "job_type", nullable = false, length = 255)
    private String jobType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "priority", nullable = false)
    private Integer priority = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt = Instant.now();

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "retry_policy", columnDefinition = "JSONB")
    private String retryPolicy;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "worker_id", length = 255)
    private String workerId;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "last_execution_started_at")
    private Instant lastExecutionStartedAt;

    @Column(name = "last_execution_completed_at")
    private Instant lastExecutionCompletedAt;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "last_error_details", columnDefinition = "JSONB")
    private String lastErrorDetails;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Job() {
        // JPA
    }

    public Job(Tenant tenant, String jobType, JsonNode payload) {
        this.tenant = tenant;
        this.jobType = jobType;
        this.payload = payload != null ? payload.toString() : "{}";
        this.status = JobStatus.PENDING;
        this.scheduledAt = Instant.now();
        this.priority = 5;
        this.attemptCount = 0;
        this.maxAttempts = 3;
    }

    public UUID getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getJobType() {
        return jobType;
    }

    public JsonNode getPayload() {
        try {
            return OBJECT_MAPPER.readTree(payload);
        } catch (JsonProcessingException e) {
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        if (priority < 0 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 0 and 10");
        }
        this.priority = priority;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public JsonNode getRetryPolicy() {
        try {
            return retryPolicy != null ? OBJECT_MAPPER.readTree(retryPolicy) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void setRetryPolicy(JsonNode retryPolicy) {
        this.retryPolicy = retryPolicy != null ? retryPolicy.toString() : null;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
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
        try {
            return lastErrorDetails != null ? OBJECT_MAPPER.readTree(lastErrorDetails) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void schedule() {
        if (status != JobStatus.PENDING) {
            throw new IllegalStateException("Can only schedule pending jobs");
        }
        this.status = JobStatus.SCHEDULED;
    }

    public void queue() {
        if (status != JobStatus.PENDING && status != JobStatus.SCHEDULED && status != JobStatus.RETRY_WAIT) {
            throw new IllegalStateException("Can only queue pending, scheduled, or retry_wait jobs");
        }
        this.status = JobStatus.QUEUED;
    }

    public void claim(String workerId, Instant leaseExpiresAt) {
        if (status != JobStatus.QUEUED) {
            throw new IllegalStateException("Can only claim queued jobs");
        }
        this.status = JobStatus.RUNNING;
        this.workerId = workerId;
        this.claimedAt = Instant.now();
        this.leaseExpiresAt = leaseExpiresAt;
        this.lastExecutionStartedAt = Instant.now();
    }

    public void succeed() {
        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException("Can only mark running jobs as succeeded");
        }
        this.status = JobStatus.SUCCEEDED;
        this.completedAt = Instant.now();
        this.lastExecutionCompletedAt = Instant.now();
        this.leaseExpiresAt = null;
    }

    public void fail(String errorMessage, JsonNode errorDetails) {
        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException("Can only fail running jobs");
        }
        this.lastErrorMessage = errorMessage;
        this.lastErrorDetails = errorDetails != null ? errorDetails.toString() : null;
        this.lastExecutionCompletedAt = Instant.now();
        this.attemptCount++;
        this.leaseExpiresAt = null;

        if (attemptCount >= maxAttempts) {
            this.status = JobStatus.DEAD_LETTERED;
            this.completedAt = Instant.now();
        } else {
            this.status = JobStatus.RETRY_WAIT;
            // Note: scheduledAt will be set by the retry logic
        }
    }

    public void scheduleRetry(Instant scheduledAt) {
        if (status != JobStatus.RETRY_WAIT) {
            throw new IllegalStateException("Can only schedule retry for jobs in RETRY_WAIT state");
        }
        this.scheduledAt = scheduledAt;
    }

    public void cancel() {
        if (status == JobStatus.SUCCEEDED || status == JobStatus.DEAD_LETTERED) {
            throw new IllegalStateException("Cannot cancel completed jobs");
        }
        this.status = JobStatus.CANCELLED;
        this.completedAt = Instant.now();
        this.leaseExpiresAt = null;
    }

    public void retry() {
        if (status != JobStatus.RETRY_WAIT) {
            throw new IllegalStateException("Can only retry jobs in RETRY_WAIT state");
        }
        this.status = JobStatus.QUEUED;
        this.lastErrorMessage = null;
        this.lastErrorDetails = null;
        this.completedAt = null;
    }

    public boolean isRetryable() {
        return status == JobStatus.RETRY_WAIT && attemptCount < maxAttempts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Job job = (Job) o;
        return Objects.equals(tenant, job.tenant) &&
                Objects.equals(jobType, job.jobType) &&
                Objects.equals(payload, job.payload) &&
                Objects.equals(priority, job.priority) &&
                status == job.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tenant, jobType, payload, priority, status);
    }
}
