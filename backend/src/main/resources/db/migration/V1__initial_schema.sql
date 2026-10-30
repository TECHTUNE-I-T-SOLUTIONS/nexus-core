-- NEXUS CORE - Initial Database Schema
-- Phase 1: Core Domain and Persistence

-- Tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenants_name ON tenants(name);

-- Jobs table
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    job_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 5,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    retry_policy TEXT,
    timeout_seconds INTEGER,
    idempotency_key VARCHAR(255),
    worker_id VARCHAR(255),
    claimed_at TIMESTAMP,
    lease_expires_at TIMESTAMP,
    last_execution_started_at TIMESTAMP,
    last_execution_completed_at TIMESTAMP,
    last_error_message TEXT,
    last_error_details TEXT,
    completed_at TIMESTAMP,
    CONSTRAINT chk_jobs_priority CHECK (priority >= 0 AND priority <= 10),
    CONSTRAINT chk_jobs_attempt_count CHECK (attempt_count <= max_attempts)
);

CREATE INDEX idx_jobs_tenant_id ON jobs(tenant_id);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_scheduled_at ON jobs(scheduled_at);
CREATE UNIQUE INDEX idx_jobs_tenant_idempotency ON jobs(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_jobs_claimable ON jobs(status, scheduled_at) WHERE status IN ('QUEUED', 'RETRY_WAIT');
