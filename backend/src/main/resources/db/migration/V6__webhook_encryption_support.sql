-- Add webhook encryption support
-- This migration adds a column to track whether secrets are encrypted
-- Existing secrets remain plaintext (backward compatible)
-- New secrets will be encrypted if encryption is enabled

ALTER TABLE webhook_endpoints 
ADD COLUMN IF NOT EXISTS secret_encrypted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add comment for documentation
COMMENT ON COLUMN webhook_endpoints.secret IS 'Webhook secret (may be plaintext or encrypted)';
COMMENT ON COLUMN webhook_endpoints.secret_encrypted IS 'Whether the secret is encrypted at rest';
