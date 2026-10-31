package com.techtune.nexus.job.persistence;

import com.techtune.nexus.job.domain.JobExecutionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionAttemptRepository extends JpaRepository<JobExecutionAttempt, UUID> {

    List<JobExecutionAttempt> findByJobId(UUID jobId);

    List<JobExecutionAttempt> findByWorkerId(String workerId);
}
