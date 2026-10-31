-- Add schedules table for recurring job scheduling
CREATE TABLE schedules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    job_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    next_execution_at TIMESTAMP NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    job_priority INTEGER NOT NULL DEFAULT 5,
    job_max_attempts INTEGER NOT NULL DEFAULT 3,
    job_timeout_seconds INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedules_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT chk_schedules_priority CHECK (job_priority >= 0 AND job_priority <= 10),
    CONSTRAINT chk_schedules_max_attempts CHECK (job_max_attempts >= 1),
    CONSTRAINT chk_schedules_timezone CHECK (timezone <> '')
);

CREATE INDEX idx_schedules_tenant ON schedules(tenant_id);
CREATE INDEX idx_schedules_enabled_next ON schedules(is_enabled, next_execution_at) WHERE is_enabled = TRUE;
CREATE INDEX idx_schedules_next_execution ON schedules(next_execution_at) WHERE is_enabled = TRUE;

-- Add schedule_id to jobs table for tracking which schedule generated the job
-- This enables schedule-level duplicate prevention
ALTER TABLE jobs ADD COLUMN schedule_id UUID REFERENCES schedules(id) ON DELETE SET NULL;

-- Add unique constraint to prevent duplicate job generation for same schedule at same time
-- This ensures that multiple scheduler instances cannot generate duplicate jobs
CREATE UNIQUE INDEX uq_jobs_schedule_time ON jobs(schedule_id, scheduled_at) WHERE schedule_id IS NOT NULL;
