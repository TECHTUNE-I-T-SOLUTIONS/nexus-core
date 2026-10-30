package com.techtune.nexus.job.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.domain.JobStatus;
import com.techtune.nexus.job.persistence.JobRepository;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class JobService {

    private final JobRepository jobRepository;
    private final TenantRepository tenantRepository;

    public JobService(JobRepository jobRepository, TenantRepository tenantRepository) {
        this.jobRepository = jobRepository;
        this.tenantRepository = tenantRepository;
    }

    public Job createJob(UUID tenantId, String jobType, JsonNode payload, Integer priority, Instant scheduledAt, Integer maxAttempts, Integer timeoutSeconds, String idempotencyKey) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        Job job = new Job(tenant, jobType, payload);
        if (priority != null) {
            job.setPriority(priority);
        }
        if (scheduledAt != null) {
            job.setScheduledAt(scheduledAt);
        }
        if (maxAttempts != null) {
            job.setMaxAttempts(maxAttempts);
        }
        if (timeoutSeconds != null) {
            job.setTimeoutSeconds(timeoutSeconds);
        }
        if (idempotencyKey != null) {
            job.setIdempotencyKey(idempotencyKey);
        }
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Job getJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Job> listJobs(UUID tenantId, String status) {
        if (tenantId != null && status != null) {
            return jobRepository.findByTenantIdAndStatus(tenantId, JobStatus.valueOf(status));
        } else if (tenantId != null) {
            return jobRepository.findByTenantId(tenantId);
        } else if (status != null) {
            return jobRepository.findByStatus(JobStatus.valueOf(status));
        } else {
            return jobRepository.findAll();
        }
    }

    @Transactional(readOnly = true)
    public List<Job> getJobsByTenant(UUID tenantId) {
        return jobRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Job> getJobsByTenantAndStatus(UUID tenantId, JobStatus status) {
        return jobRepository.findByTenantIdAndStatus(tenantId, status);
    }

    public Job scheduleJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.schedule();
        return jobRepository.save(job);
    }

    public Job queueJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.queue();
        return jobRepository.save(job);
    }

    public Job claimJob(UUID jobId, String workerId, Instant leaseExpiresAt) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.claim(workerId, leaseExpiresAt);
        return jobRepository.save(job);
    }

    public Job completeJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.succeed();
        return jobRepository.save(job);
    }

    public Job failJob(UUID jobId, String errorMessage, JsonNode errorDetails) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.fail(errorMessage, errorDetails);
        return jobRepository.save(job);
    }

    public Job cancelJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.cancel();
        return jobRepository.save(job);
    }

    public Job retryJob(UUID jobId, Instant scheduledAt) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.retry(scheduledAt);
        return jobRepository.save(job);
    }

    public Job retryJob(UUID jobId) {
        return retryJob(jobId, Instant.now());
    }
}
