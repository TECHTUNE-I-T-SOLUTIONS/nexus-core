-- Phase 3: API Keys and Worker Engine Migration

-- API Keys table
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    public_key_id UUID NOT NULL UNIQUE,
    hashed_secret VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_api_keys_expires_after_created CHECK (expires_at > created_at)
);

CREATE INDEX idx_api_keys_tenant_id ON api_keys(tenant_id);
CREATE INDEX idx_api_keys_public_key_id ON api_keys(public_key_id);
CREATE INDEX idx_api_keys_expires_at ON api_keys(expires_at) WHERE expires_at IS NOT NULL;

-- API Key Scopes table
CREATE TABLE api_key_scopes (
    api_key_id UUID NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,
    scope VARCHAR(50) NOT NULL,
    PRIMARY KEY (api_key_id, scope)
);

-- Webhook Outbox table
CREATE TABLE webhook_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    webhook_url VARCHAR(2048) NOT NULL,
    payload JSONB NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_outbox_next_retry ON webhook_outbox(next_retry_at) WHERE delivered_at IS NULL;
CREATE INDEX idx_webhook_outbox_tenant_id ON webhook_outbox(tenant_id);
