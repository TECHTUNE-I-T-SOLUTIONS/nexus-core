# Phase 5: Webhooks and Event Delivery

## Date
2026-10-31

## Scope
Implement webhook endpoint management, event generation, transactional outbox pattern, webhook delivery worker, HMAC signing, and SSRF protection.

## Architecture

### Webhook Endpoint Management
- **Entity**: `WebhookEndpoint` - Tenant-owned webhook endpoints with URL, secret, and event type subscriptions
- **Repository**: `WebhookEndpointRepository` - CRUD operations with tenant isolation
- **Service**: `WebhookEndpointService` - Business logic with SSRF protection and validation
- **API**: `WebhookEndpointController` - REST endpoints with scope enforcement

### Event Contract
- **Envelope**: `WebhookEvent` - Versioned event record with stable fields
- **Event Types**:
  - `job.completed` - Terminal success
  - `job.failed` - Terminal failure
  - `job.cancelled` - Job cancellation
- **Version**: `1.0`
- **Fields**: id, eventType, eventVersion, occurredAt, tenantId, jobId, jobType, jobStatus, payload

### Transactional Outbox
- **Entity**: `WebhookOutbox` - Enhanced with event type, version, lease tracking, HTTP response fields
- **Publisher**: `WebhookEventPublisher` - Creates outbox entries transactionally with job state changes
- **Delivery Worker**: `WebhookDeliveryWorker` - Background polling worker with lease claiming and retry logic
- **Service**: `WebhookDeliveryService` - HTTP client with RestTemplate and HMAC signing

### Database Changes
- **V5 Migration**:
  - Added `webhook_endpoints` table
  - Enhanced `webhook_outbox` table with event metadata and delivery tracking
  - Added indexes for pending webhook discovery and tenant isolation

## Implementation Details

### Webhook Endpoint Management
- Tenant-scoped endpoint registration
- CRUD operations with authentication and authorization
- Scope requirements: `webhooks:read`, `webhooks:write`
- SSRF protection: Rejects localhost, private IPs, non-HTTP(S) schemes
- Secret validation: Minimum 16 characters
- Event type filtering per endpoint

### Event Generation
- Events created transactionally with job state changes
- Uses `@Transactional(propagation = MANDATORY)` to ensure transactional integrity
- Only terminal events (completed, failed, cancelled) are published
- Intermediate retryable failures do not emit terminal failure events

### Webhook Delivery
- Background polling worker with configurable interval (default: 30s)
- Batch processing (default: 10 events per poll)
- Lease-based claiming to prevent duplicate delivery
- Exponential backoff with jitter for retries
- Max retries: 3 (configurable)
- HTTP timeouts: 10s connection, 30s response

### HMAC Signing
- Algorithm: SHA-256 HMAC
- Canonical string: `secret + payload`
- Headers:
  - `X-Webhook-Id`: Outbox entry ID
  - `X-Webhook-Signature`: Base64-encoded HMAC
  - `X-Webhook-Timestamp`: Delivery timestamp
  - `X-Webhook-Event`: Event type
  - `Publisher `X-Webhook-Event-Version`: Event version
  - `X-Webhook-Delivery`: Delivery ID

### SSRF Protections
- URL scheme validation (HTTP/HTTPS only)
- Host validation (rejects empty hosts)
- Loopback rejection (localhost, 127.0.0.1, ::1)
- Private IP range rejection (10.x, 192.168.x, 172.16-31.x)
- Port validation (80-65535)

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
      response-timeout-s: 30
      lease-duration-seconds: 300
```

## API Endpoints

### Webhook Endpoints
- `POST /api/v1/webhook-endpoints` - Create endpoint (requires `webhooks:write`)
- `GET /api/v1/webhook-endpoints/{id}` - Get endpoint (requires `webhooks:read`)
- `GET /api/v1/webhook-endpoints` - List endpoints (requires `webhooks:read`)
- `PUT /api/v1/webhook-endpoints/{id}` - Update endpoint (requires `webhooks:write`)
- `DELETE /api/v1/webhook-endpoints/{id}` - Delete endpoint (requires `webhooks:write`)

## Security

### Authentication & Authorization
- All endpoints require valid API key authentication
- Scope enforcement on all operations
- Tenant isolation enforced at service layer
- Request-supplied tenant IDs are rejected when authenticated tenant differs

### Secret Management
- Secrets stored in plaintext in database (production should encrypt)
- Secrets never returned in list responses
- Secrets validated to be minimum 16 characters
- Secrets used for HMAC signature generation

### SSRF Protections
- URL scheme validation
- Host validation
- Loopback and private IP rejection
- Port range validation
- **Limitation**: DNS resolution is not validated after initial check; deployment-level egress controls recommended

## Limitations

### SSRF Protection
- DNS rebinding attacks possible after initial validation
- IPv6 not explicitly validated
- Deployment-level egress controls recommended for production

### Event Delivery
- No guaranteed ordering of events
- At-least-once delivery semantics (duplicates possible)
- Receivers must implement idempotency
- No dead letter queue (failed events remain in outbox)

### Scheduler Integration
- Webhook event publisher exists but not yet integrated with job lifecycle
- Events must be manually triggered by calling `WebhookEventPublisher` methods
- Automatic integration with JobService pending

### Testing
- Unit tests: Not implemented (due to Docker/Testcontainers environment limitations)
- Integration tests: Not implemented (due to Docker/Testcontainers environment limitations)
- Manual testing required for verification

## Migration

### V5 Migration
- Adds `webhook_endpoints` table
- Enhances `webhook_outbox` table with event metadata
- Adds indexes for performance
- Compatible with existing V1-V4 migrations

## Dependencies

### New Dependencies
- None (uses existing Spring Boot RestTemplate)

### Existing Dependencies Used
- Spring Web (RestTemplate)
- Spring Scheduling (@Scheduled)
- Spring Transaction Management
- Jackson (JSON serialization)
- BCrypt (signature hashing)

## Deployment Notes

### Configuration
- Set `nexus.webhook.delivery.enabled=false` to disable webhook delivery
- Adjust polling interval based on workload
- Increase batch size for high-volume deployments
- Tune timeouts based on external service response times

### Monitoring
- Monitor webhook delivery success/failure rates
- Track pending webhook queue depth
- Monitor delivery worker performance
- Alert on delivery retry exhaustion

### Production Hardening
- Encrypt webhook secrets at rest
- Implement rate limiting on webhook endpoints
- Add SSL certificate validation for webhook URLs
- Implement egress controls for SSRF protection
- Add metrics for webhook delivery latency
- Implement dead letter queue for failed webhooks

## Known Issues

1. **Testcontainers not available**: Unit and integration tests could not be run due to Docker environment limitations
2. **Manual testing required**: Verification requires manual API testing with running PostgreSQL
3. **Secret encryption**: Secrets stored in plaintext; production should encrypt at rest
4. **DNS rebinding**: SSRF protection does not validate DNS resolution after initial check
5. **IPv6**: IPv6 addresses not explicitly validated
6. **Event integration**: WebhookEventPublisher not yet integrated with JobService

## Completion Status

### Implemented
- ✅ Webhook endpoint entity, repository, service, and API
- ✅ Event contract with versioning
- ✅ Transactional outbox publisher
- ✅ Webhook delivery worker with HTTP client
- ✅ HMAC signature generation
- ✅ SSRF URL validation
- ✅ Database migration
- ✅ Configuration properties
- ✅ Scope enforcement and tenant isolation

### Not Implemented
- ❌ Unit tests (Docker/Testcontainers unavailable)
- ❌ Integration tests (Docker/Testcontainers unavailable)
- ❌ Automatic integration with JobService lifecycle
- ❌ Secret encryption at rest
- ❌ Dead letter queue
- ❌ Event delivery metrics
- ❌ Manual retry API

### Next Steps
1. Add unit tests when Docker/Testcontainers environment is available
2. Integrate WebhookEventPublisher with JobService terminal state transitions
3. Implement secret encryption at rest
4. Add delivery metrics and monitoring
5. Implement dead letter queue for failed webhooks
6. Add manual retry API for exhausted deliveries
7. Enhance SSRF protection with DNS/IP validation
8. Add IPv6 validation
