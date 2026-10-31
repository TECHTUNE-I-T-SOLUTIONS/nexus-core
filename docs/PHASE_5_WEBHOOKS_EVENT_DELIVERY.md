# Phase 5: Webhooks and Event Delivery

## Date
2026-10-31 (Updated: 2026-10-31)

## Scope
Implement webhook endpoint management, event generation, transactional outbox pattern, webhook delivery worker, HMAC signing, SSRF protection, secret encryption, rate limiting, metrics, and dead-letter handling.

## Architecture

### Webhook Endpoint Management
- **Entity**: `WebhookEndpoint` - Tenant-owned webhook endpoints with URL, secret, event type subscriptions, and encryption flag
- **Repository**: `WebhookEndpointRepository` - CRUD operations with tenant isolation
- **Service**: `WebhookEndpointService` - Business logic with SSRF protection, validation, and encryption
- **API**: `WebhookEndpointController` - REST endpoints with scope enforcement and rate limiting

### Event Contract
- **Envelope**: `WebhookEvent` - Versioned event record with stable fields
- **Event Types**:
  - `job.completed` - Terminal success
  - `job.failed` - Terminal failure
  - `job.cancelled` - Job cancellation
- **Version**: `1.0`
- **Fields**: id, eventType, eventVersion, occurredAt, tenantId, jobId, jobType, jobStatus, payload

### Transactional Outbox
- **Entity**: `WebhookOutbox` - Enhanced with event type, version, lease tracking, delivery status, HTTP response fields
- **Publisher**: `WebhookEventPublisher` - Creates outbox entries transactionally with job state changes
- **Delivery Worker**: `WebhookDeliveryWorker` - Background polling worker with lease claiming, retry logic, and dead-letter handling
- **Service**: `WebhookDeliveryService` - HTTP client with RestTemplate, SSRF validation, and HMAC signing
- **Metrics**: `WebhookDeliveryMetrics` - Micrometer-based metrics for observability

### Security Components
- **SSRF Protection**: `SSRFProtection` - Comprehensive URL validation including IPv6, DNS resolution, metadata endpoints
- **Encryption**: `SecretEncryptionService` - AES-GCM encryption for webhook secrets at rest
- **Rate Limiting**: `RateLimiter` - Sliding window rate limiting per tenant

### Database Changes
- **V5 Migration**:
  - Added `webhook_endpoints` table
  - Enhanced `webhook_outbox` table with event metadata and delivery tracking
  - Added indexes for pending webhook discovery and tenant isolation
- **V6 Migration**:
  - Added `secret_encrypted` column to `webhook_endpoints` table
  - Supports mixed plaintext/encrypted secrets for safe migration

## Implementation Details

### Webhook Endpoint Management
- Tenant-scoped endpoint registration
- CRUD operations with authentication and authorization
- Scope requirements: `webhooks:read`, `webhooks:write`
- Rate limiting: 100 requests per 60 seconds per tenant (configurable)
- SSRF protection: Rejects localhost, private IPs, link-local, unique-local, IPv4-mapped IPv6, metadata endpoints
- Secret validation: Minimum 16 characters
- Event type filtering per endpoint
- Secret encryption: AES-GCM when enabled (opt-in via configuration)

### Event Generation
- Events created transactionally with job state changes
- Uses `@Transactional(propagation = MANDATORY)` to ensure transactional integrity
- Only terminal events (completed, failed, cancelled) are published
- Intermediate retryable failures do not emit terminal failure events
- Integrated with job lifecycle (completed, non-retryable failed, cancelled states)

### Webhook Delivery
- Background polling worker with configurable interval (default: 30s)
- Batch processing (default: 10 events per poll)
- Lease-based claiming to prevent duplicate delivery (PostgreSQL FOR UPDATE SKIP LOCKED)
- Lease recovery for abandoned deliveries
- Exponential backoff with jitter for retries (base 30s, max 3600s)
- Max retries: 3 (configurable)
- HTTP timeouts: 10s connection, 30s response
- Dead-letter handling after retry exhaustion
- Manual retry support for dead-lettered events

### HMAC Signing
- Algorithm: SHA-256 hash (note: currently `SHA-256(secret + payload)`, not standard HMAC)
- Headers:
  - `X-Webhook-Id`: Outbox entry ID
  - `X-Webhook-Signature`: Base64-encoded signature
  - `X-Webhook-Timestamp`: Delivery timestamp
  - `X-Webhook-Event`: Event type
  - `X-Webhook-Event-Version`: Event version
  - `X-Webhook-Delivery`: Delivery ID

### SSRF Protections
- URL scheme validation (HTTP/HTTPS only)
- User-info detection (rejects credentials in URL)
- Host validation (rejects empty hosts)
- Loopback rejection (localhost, 127.0.0.1, ::1, IPv6 loopback)
- Private IP range rejection (10.x, 192.168.x, 172.16-31.x)
- Link-local rejection (169.254.x.x, fe80::/10)
- Unique-local rejection (fc00::/7)
- IPv4-mapped IPv6 rejection (::ffff:x.x.x.x)
- Cloud metadata endpoint rejection (169.254.169.254, metadata.google.internal, etc.)
- Port validation (80-65535)
- DNS resolution validation at connection time (DNS rebinding protection)
- **Limitation**: RestTemplate follows redirects by default; deployment-level egress controls recommended

### Secret Encryption
- AES-GCM encryption for webhook secrets at rest
- Key derivation via SHA-256 from configured key
- Random IV per encryption (different ciphertext each time)
- Opt-in via configuration (`nexus.webhook.encryption.enabled=true`)
- Migration-safe: existing plaintext secrets remain functional
- Encryption flag tracked in database for decryption decision

### Rate Limiting
- Sliding window algorithm per tenant
- In-memory only (not cluster-wide)
- Configurable max requests and window duration
- Returns HTTP 429 (Too Many Requests) when rate limited
- **Limitation**: For cluster-wide rate limiting, use Redis or distributed cache

### Metrics
- `webhook.deliveries.successful` (counter)
- `webhook.deliveries.failed` (counter)
- `webhook.deliveries.retried` (counter)
- `webhook.deliveries.dead_lettered` (counter)
- `webhook.leases.recovered` (counter)
- `webhook.delivery.latency` (timer with p50, p90, p95, p99)
- Exposed via Micrometer/Prometheus endpoints

## Configuration

```yaml
nexus:
  webhook:
    delivery:
      enabled: true
      polling-interval: 30s
      batch-size: 10
      max-retries: 3
      connection-timeout-seconds: 10
      response-timeout-seconds: 30
      lease-duration-seconds: 300
    encryption:
      enabled: false  # Set to true to enable secret encryption
      key: ""  # Required when encryption is enabled
    rate-limit:
      max-requests: 100
      window-seconds: 60
```

## API Endpoints

### Webhook Endpoints
- `POST /api/v1/webhook-endpoints` - Create endpoint (requires `webhooks:write`, rate limited)
- `GET /api/v1/webhook-endpoints/{id}` - Get endpoint (requires `webhooks:read`)
- `GET /api/v1/webhook-endpoints` - List endpoints (requires `webhooks:read`)
- `PUT /api/v1/webhook-endpoints/{id}` - Update endpoint (requires `webhooks:write`, rate limited)
- `DELETE /api/v1/webhook-endpoints/{id}` - Delete endpoint (requires `webhooks:write`, rate limited)

## Security

### Authentication & Authorization
- All endpoints require valid API key authentication
- Scope enforcement on all operations
- Tenant isolation enforced at service layer
- Request-supplied tenant IDs are rejected when authenticated tenant differs

### Secret Management
- Secrets can be stored plaintext or encrypted (opt-in)
- Secrets never returned in list responses
- Secrets validated to be minimum 16 characters
- Secrets used for signature generation
- Encryption flag tracks whether secret is encrypted
- **Note**: Current signature uses SHA-256(secret + payload), not standard HMAC

### SSRF Protections
- URL scheme validation (HTTP/HTTPS only)
- User-info detection (rejects credentials in URL)
- Host validation (rejects empty hosts)
- Loopback rejection (localhost, 127.0.0.1, ::1, IPv6 loopback)
- Private IP range rejection (10.x, 192.168.x, 172.16-31.x)
- Link-local rejection (169.254.x.x, fe80::/10)
- Unique-local rejection (fc00::/7)
- IPv4-mapped IPv6 rejection (::ffff:x.x.x.x)
- Cloud metadata endpoint rejection (169.254.169.254, metadata.google.internal, etc.)
- Port validation (80-65535)
- DNS resolution validation at connection time
- **Limitation**: RestTemplate follows redirects by default; deployment-level egress controls recommended

### Rate Limiting
- Per-tenant sliding window rate limiting
- Configurable limits and window duration
- Returns HTTP 429 when rate limited
- **Limitation**: In-memory only (not cluster-wide)

## Limitations

### SSRF Protection
- RestTemplate follows redirects by default (not blocked)
- Deployment-level egress controls recommended for production
- DNS rebinding protection is at connection time, not continuously validated

### Event Delivery
- No guaranteed ordering of events
- At-least-once delivery semantics (duplicates possible)
- Receivers must implement idempotency
- Dead-letter queue exists but manual replay API not fully implemented

### Rate Limiting
- In-memory only (not cluster-wide)
- For multi-instance deployments, use Redis or distributed cache

### HMAC Signing
- Currently uses SHA-256(secret + payload), not standard HMAC-SHA256
- Consider upgrading to standard HMAC for better security

### Testing
- Full test suite passing (102 tests)
- PostgreSQL/Testcontainers integration tests validated
- One test skipped (intentional)

## Migration

### V5 Migration
- Adds `webhook_endpoints` table
- Enhances `webhook_outbox` table with event metadata
- Adds indexes for performance
- Compatible with existing V1-V4 migrations

### V6 Migration
- Adds `secret_encrypted` column to `webhook_endpoints` table
- Default value: FALSE (existing secrets plaintext)
- Safe for upgrades (uses IF NOT EXISTS)
- Supports mixed plaintext/encrypted secrets

## Dependencies

### New Dependencies
- None (uses existing Spring Boot RestTemplate, Micrometer)

### Existing Dependencies Used
- Spring Web (RestTemplate)
- Spring Scheduling (@Scheduled)
- Spring Transaction Management
- Jackson (JSON serialization, JavaTimeModule)
- Micrometer (metrics)
- Javax Crypto (AES-GCM encryption)

## Deployment Notes

### Configuration
- Set `nexus.webhook.delivery.enabled=false` to disable webhook delivery
- Set `nexus.webhook.encryption.enabled=true` and provide `nexus.webhook.encryption.key` to enable secret encryption
- Adjust polling interval based on workload
- Increase batch size for high-volume deployments
- Tune timeouts based on external service response times
- Adjust rate limiting based on tenant traffic patterns

### Monitoring
- Monitor webhook delivery success/failure rates via metrics
- Track pending webhook queue depth
- Monitor delivery worker performance
- Alert on delivery retry exhaustion
- Track dead-letter queue size
- Monitor delivery latency percentiles

### Production Hardening
- ✅ Encrypt webhook secrets at rest (opt-in via configuration)
- ✅ Implement rate limiting on webhook endpoints
- ✅ Add SSRF protection with DNS/IP validation
- ✅ Add metrics for webhook delivery latency
- ✅ Implement dead letter queue for failed webhooks
- ⚠️ Add SSL certificate validation for webhook URLs (uses JVM defaults)
- ⚠️ Implement egress controls for SSRF protection (deployment-level)
- ⚠️ Block HTTP redirects (requires custom HTTP client configuration)
- ⚠️ Upgrade to standard HMAC-SHA256 for signature

## Known Issues

1. **Redirect following**: RestTemplate follows redirects by default; not blocked by SSRF protection
2. **HMAC non-standard**: Current signature uses SHA-256(secret + payload), not standard HMAC-SHA256
3. **Rate limiting**: In-memory only (not cluster-wide)
4. **Manual replay**: Dead-letter manual replay API not fully implemented
5. **Redirect blocking**: Requires custom HTTP client configuration (not implemented)

## Completion Status

### Implemented
- ✅ Webhook endpoint entity, repository, service, and API
- ✅ Event contract with versioning
- ✅ Transactional outbox publisher
- ✅ Webhook delivery worker with HTTP client
- ✅ Signature generation (SHA-256)
- ✅ Comprehensive SSRF protection (IPv6, DNS, metadata endpoints)
- ✅ Database migrations (V5, V6)
- ✅ Configuration properties
- ✅ Scope enforcement and tenant isolation
- ✅ Unit tests (102 tests passing)
- ✅ Integration tests (PostgreSQL/Testcontainers)
- ✅ Secret encryption at rest (opt-in)
- ✅ Rate limiting (in-memory)
- ✅ Delivery metrics (Micrometer)
- ✅ Dead-letter queue and handling
- ✅ Lease recovery for abandoned deliveries
- ✅ Job lifecycle integration

### Not Implemented
- ❌ Standard HMAC-SHA256 (uses SHA-256(secret + payload))
- ❌ HTTP redirect blocking (RestTemplate follows redirects)
- ❌ Cluster-wide rate limiting (requires Redis)
- ❌ Manual replay API for dead-letter events
- ❌ SSL certificate validation customization

### Remaining Risks
1. **Redirect SSRF**: RestTemplate follows redirects; deployment-level egress controls required
2. **Rate limiting**: Not cluster-wide; use Redis for multi-instance deployments
3. **Signature contract**: Non-standard HMAC; receivers must match implementation
4. **Secret encryption**: Opt-in; existing plaintext secrets remain unencrypted

### Next Steps
1. Upgrade to standard HMAC-SHA256 for signature
2. Implement custom HTTP client to block redirects
3. Add Redis-backed rate limiting for cluster deployments
4. Implement manual replay API for dead-letter events
5. Add migration script to encrypt existing plaintext secrets
6. Add integration tests for SSRF protection with actual DNS resolution
