-- Add webhook endpoints table for tenant-specific webhook configuration
CREATE TABLE webhook_endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    url VARCHAR(2048) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    event_types TEXT NOT NULL, -- JSON array of event types
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_endpoints_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT chk_webhook_endpoints_url CHECK (url <> ''),
    CONSTRAINT chk_webhook_endpoints_url_scheme CHECK (url LIKE 'https://%' OR url LIKE 'http://%'),
    CONSTRAINT chk_webhook_endpoints_secret CHECK (secret <> '')
);

CREATE INDEX idx_webhook_endpoints_tenant ON webhook_endpoints(tenant_id);
CREATE INDEX idx_webhook_endpoints_enabled ON webhook_endpoints(is_enabled) WHERE is_enabled = TRUE;

-- Update webhook_outbox to add more delivery tracking fields
ALTER TABLE webhook_outbox 
ADD COLUMN IF NOT EXISTS event_type VARCHAR(100),
ADD COLUMN IF NOT EXISTS event_version VARCHAR(20) DEFAULT '1.0',
ADD COLUMN IF NOT EXISTS lease_expires_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS http_status INTEGER,
ADD COLUMN IF NOT EXISTS response_headers TEXT,
ADD COLUMN IF NOT EXISTS response_body TEXT,
ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(50) DEFAULT 'PENDING';

-- Add index for finding pending webhook deliveries
CREATE INDEX IF NOT EXISTS idx_webhook_outbox_pending ON webhook_outbox(next_retry_at) WHERE delivered_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_webhook_outbox_tenant ON webhook_outbox(tenant_id);
CREATE INDEX IF NOT EXISTS idx_webhook_outbox_delivery_status ON webhook_outbox(delivery_status);
