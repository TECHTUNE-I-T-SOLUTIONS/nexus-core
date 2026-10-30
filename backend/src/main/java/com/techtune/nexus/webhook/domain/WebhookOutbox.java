package com.techtune.nexus.webhook.domain;

import com.techtune.nexus.common.domain.UuidIdentity;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.tenant.domain.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_outbox")
public class WebhookOutbox extends UuidIdentity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, updatable = false)
    private Job job;

    @Column(name = "webhook_url", nullable = false, length = 2048)
    private String webhookUrl;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public WebhookOutbox() {
    }

    public WebhookOutbox(Tenant tenant, Job job, String webhookUrl, String payload) {
        this.tenant = tenant;
        this.job = job;
        this.webhookUrl = webhookUrl;
        this.payload = payload;
        this.nextRetryAt = Instant.now();
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Job getJob() {
        return job;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getPayload() {
        return payload;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void scheduleRetry(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public void markDelivered() {
        this.deliveredAt = Instant.now();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isRetryable() {
        return retryCount < maxRetries && deliveredAt == null;
    }

    public boolean isDelivered() {
        return deliveredAt != null;
    }
}
