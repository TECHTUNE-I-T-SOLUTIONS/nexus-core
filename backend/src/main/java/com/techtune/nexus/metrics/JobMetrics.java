package com.techtune.nexus.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JobMetrics {

    private final Counter jobsSubmitted;
    private final Counter jobsClaimed;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter jobsRetried;
    private final Counter jobsExpired;
    private final Counter webhooksDelivered;
    private final Counter webhooksFailed;
    private final Timer jobExecutionTimer;

    public JobMetrics(MeterRegistry registry) {
        this.jobsSubmitted = Counter.builder("nexus.jobs.submitted")
                .description("Total number of jobs submitted")
                .register(registry);
        
        this.jobsClaimed = Counter.builder("nexus.jobs.claimed")
                .description("Total number of jobs claimed by workers")
                .register(registry);
        
        this.jobsCompleted = Counter.builder("nexus.jobs.completed")
                .description("Total number of jobs completed successfully")
                .register(registry);
        
        this.jobsFailed = Counter.builder("nexus.jobs.failed")
                .description("Total number of jobs failed")
                .register(registry);
        
        this.jobsRetried = Counter.builder("nexus.jobs.retried")
                .description("Total number of jobs retried")
                .register(registry);
        
        this.jobsExpired = Counter.builder("nexus.jobs.expired")
                .description("Total number of job leases expired")
                .register(registry);
        
        this.webhooksDelivered = Counter.builder("nexus.webhooks.delivered")
                .description("Total number of webhooks delivered successfully")
                .register(registry);
        
        this.webhooksFailed = Counter.builder("nexus.webhooks.failed")
                .description("Total number of webhook delivery failures")
                .register(registry);
        
        this.jobExecutionTimer = Timer.builder("nexus.jobs.execution.duration")
                .description("Job execution duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofMinutes(5))
                .register(registry);
    }

    public void incrementJobsSubmitted() {
        jobsSubmitted.increment();
    }

    public void incrementJobsClaimed() {
        jobsClaimed.increment();
    }

    public void incrementJobsCompleted() {
        jobsCompleted.increment();
    }

    public void incrementJobsFailed() {
        jobsFailed.increment();
    }

    public void incrementJobsRetried() {
        jobsRetried.increment();
    }

    public void incrementJobsExpired() {
        jobsExpired.increment();
    }

    public void incrementWebhooksDelivered() {
        webhooksDelivered.increment();
    }

    public void incrementWebhooksFailed() {
        webhooksFailed.increment();
    }

    public Timer.Sample startJobExecution() {
        return Timer.start();
    }

    public void recordJobExecution(Timer.Sample sample) {
        sample.stop(jobExecutionTimer);
    }
}
