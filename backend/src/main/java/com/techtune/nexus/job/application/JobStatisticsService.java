package com.techtune.nexus.job.application;

import com.techtune.nexus.job.domain.JobStatus;
import com.techtune.nexus.job.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobStatisticsService {

    private final JobRepository jobRepository;

    public JobStatisticsService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public JobStatistics getStatistics(UUID tenantId) {
        return new JobStatistics(
            jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.QUEUED),
            jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.RUNNING),
            jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.SUCCEEDED),
            jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.FAILED),
            jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.CANCELLED),
            jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.RETRY_WAIT)
        );
    }

    public record JobStatistics(
        long queued,
        long running,
        long succeeded,
        long failed,
        long cancelled,
        long retryWait
    ) {}
}
