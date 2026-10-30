package com.techtune.nexus.worker.engine;

import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.domain.JobStatus;
import com.techtune.nexus.job.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobExecutor {

    private final JobRepository jobRepository;

    public JobExecutor(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public void executeJob(UUID jobId, String workerId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        if (!job.getStatus().equals(JobStatus.QUEUED) && !job.getStatus().equals(JobStatus.RETRY_WAIT)) {
            throw new IllegalStateException("Job is not in a claimable state");
        }

        // Move RETRY_WAIT to QUEUED before claiming
        if (job.getStatus().equals(JobStatus.RETRY_WAIT)) {
            job.queue();
            jobRepository.save(job);
        }

        // Claim the job
        job.claim(workerId, Instant.now().plusSeconds(60)); // 60 second lease
        jobRepository.save(job);

        try {
            // TODO: Execute actual job handler
            // executeHandler(job);
            
            job.succeed();
        } catch (Exception e) {
            job.fail(e.getMessage(), null);
            
            if (job.getAttemptCount() < job.getMaxAttempts()) {
                job.retry(Instant.now().plusSeconds(60)); // 1 minute backoff
            }
        }

        jobRepository.save(job);
    }
}
