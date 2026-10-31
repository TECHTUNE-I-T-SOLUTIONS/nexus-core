-- Phase 3: Execution Attempt Tracking
CREATE TABLE job_execution_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    worker_id VARCHAR(255) NOT NULL,
    attempt_number INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    outcome VARCHAR(50) NOT NULL, -- 'SUCCESS', 'FAILURE', 'TIMEOUT'
    error_message TEXT,
    error_details TEXT,
    lease_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_execution_attempts_job_id ON job_execution_attempts(job_id);
CREATE INDEX idx_job_execution_attempts_worker_id ON job_execution_attempts(worker_id);
CREATE INDEX idx_job_execution_attempts_started_at ON job_execution_attempts(started_at);
