package com.techtune.nexus.worker.engine;

import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.job.persistence.JobRepository;
import com.techtune.nexus.metrics.JobMetrics;
import com.techtune.nexus.worker.config.WorkerProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class Worker {

    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    private final WorkerProperties properties;
    private final JobClaimingService claimingService;
    private final JobExecutor executor;
    private final JobRepository jobRepository;
    private final JobMetrics metrics;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<UUID, String> activeJobs = new ConcurrentHashMap<>();

    private volatile boolean running = true;

    public Worker(WorkerProperties properties,
                  JobClaimingService claimingService,
                  JobExecutor executor,
                  JobRepository jobRepository,
                  JobMetrics metrics) {
        this.properties = properties;
        this.claimingService = claimingService;
        this.executor = executor;
        this.jobRepository = jobRepository;
        this.metrics = metrics;
        this.executorService = Executors.newFixedThreadPool(properties.getConcurrency());
    }

    @Scheduled(fixedDelayString = "${nexus.worker.pollingInterval:5s}", initialDelayString = "10s")
    public void pollAndExecute() {
        if (!properties.isEnabled() || !running) {
            return;
        }

        try {
            String workerId = properties.getWorkerId();
            int batchSize = properties.getBatchSize();
            int leaseDuration = properties.getLeaseDurationSeconds();

            // For now, we'll claim from all tenants (null tenantId)
            // In production, you might want tenant-specific workers
            List<Job> claimedJobs = claimingService.claimJobs(null, workerId, batchSize, leaseDuration);

            if (claimedJobs.isEmpty()) {
                logger.debug("No jobs to claim");
                return;
            }

            logger.info("Claimed {} jobs", claimedJobs.size());

            // Execute jobs concurrently
            for (Job job : claimedJobs) {
                activeJobs.put(job.getId(), workerId);
                executorService.submit(() -> {
                    try {
                        executor.executeJob(job.getId(), workerId);
                    } catch (Exception e) {
                        logger.error("Failed to execute job {}", job.getId(), e);
                    } finally {
                        activeJobs.remove(job.getId());
                    }
                });
            }

        } catch (Exception e) {
            logger.error("Error during worker polling", e);
        }
    }

    @Scheduled(fixedDelayString = "60s", initialDelayString = "30s")
    public void recoverExpiredLeases() {
        if (!properties.isEnabled() || !running) {
            return;
        }

        try {
            List<Job> expiredJobs = claimingService.findExpiredLeases();
            
            if (expiredJobs.isEmpty()) {
                return;
            }

            logger.info("Found {} expired leases to recover", expiredJobs.size());

            for (Job job : expiredJobs) {
                // Verify this worker doesn't own the job (prevent self-recovery)
                if (!properties.getWorkerId().equals(job.getWorkerId())) {
                    claimingService.recoverJob(job, properties.getWorkerId());
                }
            }

        } catch (Exception e) {
            logger.error("Error during lease recovery", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down worker...");
        running = false;
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Worker shutdown complete");
    }

    public boolean isRunning() {
        return running;
    }

    public int getActiveJobCount() {
        return activeJobs.size();
    }
}
