package com.techtune.nexus.worker.engine;

import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.domain.JobExecutionAttempt;
import com.techtune.nexus.job.domain.JobStatus;
import com.techtune.nexus.job.persistence.JobExecutionAttemptRepository;
import com.techtune.nexus.job.persistence.JobRepository;
import com.techtune.nexus.metrics.JobMetrics;
import com.techtune.nexus.worker.handlers.JobHandler;
import com.techtune.nexus.worker.handlers.JobHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobExecutor {

    private final JobRepository jobRepository;
    private final JobExecutionAttemptRepository attemptRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final JobMetrics jobMetrics;

    public JobExecutor(JobRepository jobRepository, 
                      JobExecutionAttemptRepository attemptRepository,
                      JobHandlerRegistry handlerRegistry, 
                      JobMetrics jobMetrics) {
        this.jobRepository = jobRepository;
        this.attemptRepository = attemptRepository;
        this.handlerRegistry = handlerRegistry;
        this.jobMetrics = jobMetrics;
    }

    @Transactional
    public void executeJob(UUID jobId, String workerId) {
        Instant startTime = Instant.now();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        // Verify job is in RUNNING state and owned by this worker
        if (!job.getStatus().equals(JobStatus.RUNNING)) {
            throw new IllegalStateException("Job is not in RUNNING state");
        }

        if (!workerId.equals(job.getWorkerId())) {
            throw new IllegalStateException("Job is not owned by this worker");
        }

        // Verify lease is still valid
        if (job.getLeaseExpiresAt() != null && Instant.now().isAfter(job.getLeaseExpiresAt())) {
            throw new IllegalStateException("Job lease has expired");
        }

        // Create execution attempt record
        JobExecutionAttempt attempt = new JobExecutionAttempt(
            job, 
            workerId, 
            job.getAttemptCount() + 1, 
            job.getLeaseExpiresAt()
        );
        attemptRepository.save(attempt);

        // Resolve handler
        JobHandler handler;
        try {
            handler = handlerRegistry.getHandler(job.getJobType());
        } catch (IllegalArgumentException e) {
            // Unknown job type - treat as unrecoverable failure
            job.fail("Unknown job type: " + job.getJobType(), null);
            attempt.completeFailure("Unknown job type: " + job.getJobType(), null);
            attemptRepository.save(attempt);
            jobRepository.save(job);
            jobMetrics.recordFailed();
            return;
        }

        // Execute handler with metrics
        try {
            handler.handle(job);
            
            // Success transition
            job.succeed();
            attempt.completeSuccess();
            attemptRepository.save(attempt);
            jobRepository.save(job);
            jobMetrics.recordCompleted();
            jobMetrics.recordExecutionDuration(java.time.Duration.between(startTime, Instant.now()).toMillis());
            
        } catch (Exception e) {
            // Failure transition
            job.fail(e.getMessage(), null);
            attempt.completeFailure(e.getMessage(), null);
            attemptRepository.save(attempt);
            jobRepository.save(job);
            jobMetrics.recordFailed();
            jobMetrics.recordExecutionDuration(java.time.Duration.between(startTime, Instant.now()).toMillis());
            
            // Schedule retry if attempts remain
            if (job.getAttemptCount() < job.getMaxAttempts()) {
                Instant retryAt = calculateRetryDelay(job.getAttemptCount());
                job.scheduleRetry(retryAt);
                job.retry();
                jobRepository.save(job);
                jobMetrics.recordRetried();
            }
        }
    }

    private Instant calculateRetryDelay(int attemptCount) {
        // Exponential backoff: 2^attempt_count seconds, max 1 hour
        long delaySeconds = (long) Math.min(Math.pow(2, attemptCount), 3600);
        return Instant.now().plusSeconds(delaySeconds);
    }
}
