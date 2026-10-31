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

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "event_version", length = 20)
    private String eventVersion = "1.0";

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "delivery_status", length = 50)
    private String deliveryStatus = "PENDING";

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

    public String getEventType() {
        return eventType;
    }

    public String getEventVersion() {
        return eventVersion;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventVersion(String eventVersion) {
        this.eventVersion = eventVersion;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setPayload(String payload) {
        this.payload = payload;
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

    public void markDeadLettered() {
        this.deliveryStatus = "DEAD_LETTER";
        this.deliveredAt = Instant.now();
    }

    public void markForRetry() {
        this.deliveryStatus = "PENDING";
        this.retryCount = 0;
        this.nextRetryAt = Instant.now();
        this.leaseExpiresAt = null;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }
}
