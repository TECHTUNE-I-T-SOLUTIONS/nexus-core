# Phase 5 Webhook Outbox Status

## Date
2026-10-31

## Current Implementation Status: Foundation Only

### What Exists (Phase 3)
- **WebhookOutbox Entity**: Database model for transactional webhook delivery
- **WebhookOutboxRepository**: JPA repository for persistence
- **Database Schema**: V2 migration includes `webhook_outbox` table
- **Domain Logic**: Retry counting, delivery tracking, error handling methods

### What is NOT Implemented (Required for Phase 5)
1. **Webhook Delivery Service**: HTTP client to actually send webhooks
2. **Webhook Delivery Scheduler**: Background process to poll and send pending webhooks
3. **Webhook Configuration**: Tenant-specific webhook URL registration
4. **HTTP Client**: Configurable HTTP client with timeouts and retry logic
5. **Signature Verification**: HMAC or similar webhook signature validation
6. **Event Publishing**: Integration with job completion events to trigger webhooks
7. **Dead Letter Handling**: Failed webhook recovery or manual retry mechanisms

### Database Schema (V2 Migration)
```sql
CREATE TABLE webhook_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    job_id UUID NOT NULL REFERENCES jobs(id),
    webhook_url VARCHAR(2048) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Dependencies Required for Phase 5
- **HTTP Client**: Spring WebClient or Apache HttpClient
- **Scheduling**: Quartz or Spring Scheduling for delivery polling
- **Configuration**: Webhook URL registration per tenant
- **Security**: HMAC signature generation for webhook authenticity
- **Monitoring**: Metrics for webhook delivery success/failure rates

### Integration Points
1. **Job Completion**: WebhookOutbox entries should be created when jobs complete
2. **JobExecutor**: Should publish webhook events on success/failure
3. **Worker**: Should integrate with webhook delivery scheduling

### Phase 3 Scope
The webhook outbox in Phase 3 is a **transactional foundation** only. No actual HTTP delivery is implemented. This aligns with the Phase 3 goal of focusing on worker reliability and execution, not webhook delivery.

### Next Steps for Phase 5
1. Implement WebhookDeliveryService with HTTP client
2. Implement WebhookDeliveryScheduler for background delivery
3. Add webhook URL configuration to Tenant entity
4. Integrate webhook event publishing with JobExecutor
5. Add webhook signature verification
6. Implement webhook delivery metrics
7. Add webhook delivery tests

## Conclusion
The webhook outbox is a solid foundation for Phase 5 but requires significant additional implementation to become a complete webhook delivery system.
