package com.techtune.nexus.worker.engine;

import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.domain.JobStatus;
import com.techtune.nexus.job.persistence.JobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobPollingService {

    private final JobRepository jobRepository;

    public JobPollingService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public List<Job> pollClaimableJobs(UUID tenantId, int batchSize) {
        Instant now = Instant.now();
        List<JobStatus> claimableStatuses = List.of(JobStatus.QUEUED, JobStatus.RETRY_WAIT);
        
        return jobRepository.findClaimableJobs(tenantId, claimableStatuses, now, PageRequest.of(0, batchSize));
    }

    @Transactional
    public List<Job> findExpiredLeases(JobStatus status) {
        Instant now = Instant.now();
        return jobRepository.findExpiredLeases(status, now);
    }
}
