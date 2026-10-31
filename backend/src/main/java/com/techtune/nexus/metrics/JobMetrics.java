package com.techtune.nexus.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class JobMetrics {

    private final Counter jobsSubmitted;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter jobsRetried;
    private final Counter jobsCancelled;
    private final Counter leaseRecoveries;
    private final Timer executionDuration;

    public JobMetrics(MeterRegistry registry) {
        this.jobsSubmitted = Counter.builder("nexus.jobs.submitted")
                .description("Total jobs submitted")
                .register(registry);
        
        this.jobsCompleted = Counter.builder("nexus.jobs.completed")
                .description("Total jobs completed successfully")
                .register(registry);
        
        this.jobsFailed = Counter.builder("nexus.jobs.failed")
                .description("Total jobs permanently failed")
                .register(registry);
        
        this.jobsRetried = Counter.builder("nexus.jobs.retried")
                .description("Total job retries")
                .register(registry);
        
        this.jobsCancelled = Counter.builder("nexus.jobs.cancelled")
                .description("Total jobs cancelled")
                .register(registry);
        
        this.leaseRecoveries = Counter.builder("nexus.jobs.lease_recoveries")
                .description("Total lease recovery events")
                .register(registry);
        
        this.executionDuration = Timer.builder("nexus.jobs.execution_duration")
                .description("Job execution duration")
                .register(registry);
    }

    public void recordSubmitted() {
        jobsSubmitted.increment();
    }

    public void recordCompleted() {
        jobsCompleted.increment();
    }

    public void recordFailed() {
        jobsFailed.increment();
    }

    public void recordRetried() {
        jobsRetried.increment();
    }

    public void recordCancelled() {
        jobsCancelled.increment();
    }

    public void recordLeaseRecovery() {
        leaseRecoveries.increment();
    }

    public void recordExecutionDuration(long millis) {
        executionDuration.record(millis, TimeUnit.MILLISECONDS);
    }
}
