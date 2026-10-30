# API Key Security Remediation Plan

## Current Status (Development Placeholder)

The current API key authentication in `ApiKeyAuthenticationFilter` is a **temporary development-only implementation** with the following characteristics:

### Current Implementation
- **Format**: `tenantId:apiKey` (e.g., `550e8400-e29b-41d4-a716-446655440000:secret123`)
- **Validation**: Simple format validation only
- **Storage**: No database persistence
- **Security**: No secret hashing, no expiration, no revocation

### Security Limitations
1. **Untrusted Tenant ID**: Tenant ID is extracted from client-supplied header
2. **No Secret Verification**: Any client can forge a valid format
3. **No Rate Limiting**: Unlimited requests per API key
4. **No Audit Trail**: No last-used tracking
5. **No Scopes**: All keys have full access
6. **No Expiration**: Keys never expire
7. **No Revocation**: Compromised keys cannot be disabled

### ⚠️ NOT PRODUCTION READY
This implementation must not be used in production environments.

---

## Phase 2 Production Implementation Plan

### 1. Database Schema

```sql
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    public_key_id UUID NOT NULL UNIQUE,
    hashed_secret VARCHAR(255) NOT NULL,
    scopes VARCHAR(255)[] NOT NULL DEFAULT ARRAY['*']::VARCHAR(255)[],
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
```

### 2. Security Features

#### Key Generation
- Public key identifier: UUID (exposed to client)
- Secret: 32-byte random string (hashed with bcrypt)
- Separate public/private identifiers to prevent leaking secrets

#### Authentication Flow
1. Client sends: `X-API-Key: <public_key_id>:<secret>`
2. Server looks up API key by `public_key_id`
3. Server verifies: key is active, not expired, belongs to tenant
4. Server compares secret using constant-time comparison
5. Server updates `last_used_at`
6. Server sets authentication with tenant ID and scopes

#### Security Controls
- **Hashing**: bcrypt with work factor 12
- **Constant-time comparison**: prevent timing attacks
- **Expiration**: Configurable TTL (default 90 days)
- **Revocation**: `is_active` flag
- **Rate limiting**: Per-API-key rate limits
- **Scopes**: Fine-grained permissions (e.g., `jobs:read`, `jobs:write`, `tenants:read`)
- **Audit**: `last_used_at` tracking

### 3. Implementation Components

#### Entities
- `ApiKey` entity with lifecycle management
- `ApiKeyStatus` enum (ACTIVE, REVOKED, EXPIRED)

#### Repositories
- `ApiKeyRepository` with:
  - `findByPublicKeyIdAndIsActiveTrue(UUID)`
  - `findByTenantId(UUID)`
  - `findByExpiresAtBefore(Instant)` (for cleanup)

#### Services
- `ApiKeyService`:
  - `generateApiKey(UUID tenantId, Set<String> scopes, Instant expiresAt)`
  - `validateApiKey(String publicKeyId, String secret)`
  - `revokeApiKey(UUID keyId)`
  - `rotateApiKey(UUID keyId)`
  - `cleanupExpiredKeys()`

#### Security Updates
- Update `ApiKeyAuthenticationFilter` to:
  - Use `ApiKeyService` for validation
  - Load scopes from database
  - Constant-time secret comparison
  - Set authentication with proper authorities

#### Rate Limiting
- Bucket4j or Spring RateLimiter
- Per-API-key rate limits
- Configurable per-scope limits

### 4. API Changes

#### Key Generation Endpoint
```
POST /api/v1/api-keys
Request: { "scopes": ["jobs:read", "jobs:write"], "expiresInDays": 90 }
Response: { "publicKeyId": "...", "secret": "...", "expiresAt": "..." }
```

#### Key Management Endpoints
```
GET /api/v1/api-keys
DELETE /api/v1/api-keys/{id}
POST /api/v1/api-keys/{id}/rotate
```

### 5. Migration Strategy

1. Create `api_keys` table (Flyway migration)
2. Implement `ApiKey` entity and repository
3. Implement `ApiKeyService`
4. Update `ApiKeyAuthenticationFilter`
5. Add key management endpoints
6. Add rate limiting
7. Document migration from placeholder to production
8. Add integration tests

### 6. Testing Requirements

- Unit tests for `ApiKeyService`
- Integration tests for key generation, validation, revocation
- Security tests for:
  - Constant-time comparison
  - Secret verification
  - Expiration enforcement
  - Revocation enforcement
  - Scope enforcement
  - Rate limiting

---

## Timeline

This should be implemented in **Phase 2** after the current remediation is complete.
