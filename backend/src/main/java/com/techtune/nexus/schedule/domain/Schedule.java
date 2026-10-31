package com.techtune.nexus.schedule.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techtune.nexus.common.domain.UuidIdentity;
import com.techtune.nexus.tenant.domain.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "schedules")
public class Schedule extends UuidIdentity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "job_type", nullable = false, length = 255)
    private String jobType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(name = "next_execution_at", nullable = false)
    private Instant nextExecutionAt;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "job_priority", nullable = false)
    private Integer jobPriority = 5;

    @Column(name = "job_max_attempts", nullable = false)
    private Integer jobMaxAttempts = 3;

    @Column(name = "job_timeout_seconds")
    private Integer jobTimeoutSeconds;

    protected Schedule() {
        // JPA
    }

    public Schedule(Tenant tenant, String jobType, JsonNode payload, String cronExpression, String timezone) {
        this.tenant = tenant;
        this.jobType = jobType;
        this.payload = payload != null ? payload.toString() : "{}";
        this.cronExpression = cronExpression;
        this.timezone = timezone != null ? timezone : "UTC";
        this.enabled = true;
        this.jobPriority = 5;
        this.jobMaxAttempts = 3;
    }

    public UUID getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public JsonNode getPayload() {
        try {
            return OBJECT_MAPPER.readTree(payload);
        } catch (JsonProcessingException e) {
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload != null ? payload.toString() : "{}";
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Instant getNextExecutionAt() {
        return nextExecutionAt;
    }

    public void setNextExecutionAt(Instant nextExecutionAt) {
        this.nextExecutionAt = nextExecutionAt;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getJobPriority() {
        return jobPriority;
    }

    public void setJobPriority(Integer jobPriority) {
        this.jobPriority = jobPriority;
    }

    public Integer getJobMaxAttempts() {
        return jobMaxAttempts;
    }

    public void setJobMaxAttempts(Integer jobMaxAttempts) {
        this.jobMaxAttempts = jobMaxAttempts;
    }

    public Integer getJobTimeoutSeconds() {
        return jobTimeoutSeconds;
    }

    public void setJobTimeoutSeconds(Integer jobTimeoutSeconds) {
        this.jobTimeoutSeconds = jobTimeoutSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Schedule schedule = (Schedule) o;
        return Objects.equals(tenant, schedule.tenant) &&
                Objects.equals(jobType, schedule.jobType) &&
                Objects.equals(cronExpression, schedule.cronExpression) &&
                Objects.equals(enabled, schedule.enabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tenant, jobType, cronExpression, enabled);
    }
}
