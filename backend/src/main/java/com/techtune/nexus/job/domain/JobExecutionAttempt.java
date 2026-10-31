package com.techtune.nexus.job.domain;

import com.techtune.nexus.common.domain.UuidIdentity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_execution_attempts")
public class JobExecutionAttempt extends UuidIdentity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, updatable = false)
    private Job job;

    @Column(name = "worker_id", nullable = false, length = 255)
    private String workerId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "outcome", nullable = false, length = 50)
    private String outcome;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    public JobExecutionAttempt() {
    }

    public JobExecutionAttempt(Job job, String workerId, Integer attemptNumber, Instant leaseExpiresAt) {
        this.job = job;
        this.workerId = workerId;
        this.attemptNumber = attemptNumber;
        this.startedAt = Instant.now();
        this.leaseExpiresAt = leaseExpiresAt;
        this.outcome = "RUNNING";
    }

    public Job getJob() {
        return job;
    }

    public String getWorkerId() {
        return workerId;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void completeSuccess() {
        this.outcome = "SUCCESS";
        this.completedAt = Instant.now();
    }

    public void completeFailure(String errorMessage, String errorDetails) {
        this.outcome = "FAILURE";
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
    }

    public void completeTimeout() {
        this.outcome = "TIMEOUT";
        this.completedAt = Instant.now();
    }
}
