package com.techtune.nexus.job.persistence;

import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query("SELECT j FROM Job j WHERE j.tenant.id = :tenantId")
    List<Job> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT j FROM Job j WHERE j.tenant.id = :tenantId AND j.status = :status")
    List<Job> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") JobStatus status);

    List<Job> findByStatus(JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.tenant.id = :tenantId AND j.idempotencyKey = :idempotencyKey")
    Optional<Job> findByTenantIdAndIdempotencyKey(@Param("tenantId") UUID tenantId, @Param("idempotencyKey") String idempotencyKey);

    @Query("SELECT COUNT(j) > 0 FROM Job j WHERE j.tenant.id = :tenantId AND j.idempotencyKey = :idempotencyKey")
    boolean existsByTenantIdAndIdempotencyKey(@Param("tenantId") UUID tenantId, @Param("idempotencyKey") String idempotencyKey);

    @Query("SELECT j FROM Job j WHERE j.tenant.id = :tenantId AND j.status IN :statuses AND j.scheduledAt <= :now ORDER BY j.priority DESC, j.createdAt ASC")
    List<Job> findClaimableJobs(@Param("tenantId") UUID tenantId,
                                @Param("statuses") List<JobStatus> statuses,
                                @Param("now") Instant now);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.leaseExpiresAt < :now")
    List<Job> findExpiredLeases(@Param("status") JobStatus status, @Param("now") Instant now);

    @Modifying
    @Query(value = """
        UPDATE jobs 
        SET status = 'RUNNING',
            worker_id = :workerId,
            claimed_at = CURRENT_TIMESTAMP,
            lease_expires_at = :leaseExpiresAt,
            last_execution_started_at = CURRENT_TIMESTAMP
        WHERE id = :jobId 
          AND status = 'QUEUED'
          AND scheduled_at <= :now
        RETURNING *
        """, nativeQuery = true)
    List<Job> claimJob(@Param("jobId") UUID jobId,
                       @Param("workerId") String workerId,
                       @Param("leaseExpiresAt") Instant leaseExpiresAt,
                       @Param("now") Instant now);

    @Modifying
    @Query(value = """
        UPDATE jobs 
        SET status = 'RUNNING',
            worker_id = :workerId,
            claimed_at = CURRENT_TIMESTAMP,
            lease_expires_at = :leaseExpiresAt,
            last_execution_started_at = CURRENT_TIMESTAMP
        WHERE id IN (
            SELECT id FROM jobs 
            WHERE tenant_id = :tenantId
              AND (status = 'QUEUED' OR (status = 'RETRY_WAIT' AND scheduled_at <= :now))
            ORDER BY priority DESC, created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        )
        RETURNING *
        """, nativeQuery = true)
    List<Job> claimJobsBatch(@Param("tenantId") UUID tenantId,
                            @Param("workerId") String workerId,
                            @Param("leaseExpiresAt") Instant leaseExpiresAt,
                            @Param("now") Instant now,
                            @Param("batchSize") int batchSize);
}
