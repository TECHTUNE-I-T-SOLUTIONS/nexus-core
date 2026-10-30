package com.techtune.nexus.worker.engine;

import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.persistence.JobRepository;
import com.techtune.nexus.metrics.JobMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobClaimingService {

    private final JobRepository jobRepository;
    private final JobMetrics metrics;

    public JobClaimingService(JobRepository jobRepository, JobMetrics metrics) {
        this.jobRepository = jobRepository;
        this.metrics = metrics;
    }

    /**
     * Atomically claim a batch of jobs for a worker using PostgreSQL row-level locking.
     * This prevents multiple workers from claiming the same job concurrently.
     * 
     * @param tenantId The tenant ID (null for global worker)
     * @param workerId The worker identifier
     * @param batchSize Maximum number of jobs to claim
     * @param leaseDurationSeconds Duration of the lease in seconds
     * @return List of claimed jobs
     */
    @Transactional
    public List<Job> claimJobs(UUID tenantId, String workerId, int batchSize, int leaseDurationSeconds) {
        Instant now = Instant.now();
        Instant leaseExpiresAt = now.plusSeconds(leaseDurationSeconds);

        List<Job> claimedJobs;
        if (tenantId != null) {
            // Tenant-specific claiming
            claimedJobs = jobRepository.claimJobsBatch(tenantId, workerId, leaseExpiresAt, now, batchSize);
        } else {
            // Global claiming - iterate through tenants (this is a simple implementation)
            // In production, you might want a more sophisticated approach
            claimedJobs = List.of(); // For now, global claiming is not implemented
        }

        // Record metrics
        claimedJobs.forEach(job -> metrics.incrementJobsClaimed());

        return claimedJobs;
    }

    /**
     * Atomically claim a single job by ID.
     * 
     * @param jobId The job ID
     * @param workerId The worker identifier
     * @param leaseDurationSeconds Duration of the lease in seconds
     * @return The claimed job, or null if it couldn't be claimed
     */
    @Transactional
    public Job claimJob(UUID jobId, String workerId, int leaseDurationSeconds) {
        Instant now = Instant.now();
        Instant leaseExpiresAt = now.plusSeconds(leaseDurationSeconds);

        List<Job> claimed = jobRepository.claimJob(jobId, workerId, leaseExpiresAt, now);
        
        if (!claimed.isEmpty()) {
            metrics.incrementJobsClaimed();
            return claimed.get(0);
        }
        
        return null;
    }

    /**
     * Find jobs with expired leases that need recovery.
     * 
     * @return List of jobs with expired leases
     */
    @Transactional(readOnly = true)
    public List<Job> findExpiredLeases() {
        return jobRepository.findExpiredLeases(com.techtune.nexus.job.domain.JobStatus.RUNNING, Instant.now());
    }

    @Transactional
    public void recoverJob(Job job, String workerId) {
        // Prevent self-recovery
        if (workerId.equals(job.getWorkerId())) {
            return;
        }

        job.fail("Lease expired", null);
        
        if (job.getAttemptCount() < job.getMaxAttempts()) {
            job.scheduleRetry(Instant.now().plusSeconds(60));
            job.retry();
            metrics.incrementJobsRetried();
        } else {
            metrics.incrementJobsExpired();
        }
        
        jobRepository.save(job);
        metrics.incrementJobsRecovered();
    }
}
