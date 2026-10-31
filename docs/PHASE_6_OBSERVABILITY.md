# Phase 6: Observability and Operational Readiness

## Overview

Phase 6 adds observability and operational readiness to NEXUS CORE, enabling monitoring, metrics, and operational statistics for production deployment.

## Changes Implemented

### 1. Job Metrics

**File**: `JobMetrics.java`

Added comprehensive Micrometer metrics for job processing:
- `nexus.jobs.submitted` - Counter for total jobs submitted
- `nexus.jobs.completed` - Counter for jobs completed successfully
- `nexus.jobs.failed` - Counter for jobs permanently failed
- `nexus.jobs.retried` - Counter for job retries
- `nexus.jobs.cancelled` - Counter for jobs cancelled
- `nexus.jobs.lease_recoveries` - Counter for lease recovery events
- `nexus.jobs.execution_duration` - Timer for job execution duration

**Integration**:
- `JobService` - Records submitted, completed, failed, cancelled metrics
- `JobExecutor` - Records completed, failed, retried metrics and execution duration

### 2. Execution Statistics Service

**File**: `JobStatisticsService.java`

Added service for operational execution statistics:
- Counts by job status (queued, running, succeeded, failed, cancelled, retry_wait)
- Tenant-scoped statistics
- Read-only transactions for performance

**Repository Changes**:
- Added `countByTenantIdAndStatus` query to `JobRepository`

### 3. Prometheus Metrics Export

**File**: `pom.xml`

Added Micrometer Prometheus registry dependency for metrics export.

## Existing Observability Features

### Spring Boot Actuator

**Endpoints** (from `application.yaml`):
- `/actuator/health` - Health checks
- `/actuator/info` - Application information
- `/actuator/metrics` - Micrometer metrics
- `/actuator/prometheus` - Prometheus metrics export

**Security**: `show-details: when-authorized` - Details only shown to authorized users

### Webhook Metrics

**File**: `WebhookDeliveryMetrics.java`

Existing webhook delivery metrics:
- Delivery attempts
- Successful deliveries
- Failed deliveries
- Retries
- Dead-letter transitions
- Lease recoveries
- Delivery latency

### Logging

**Configuration** (from `application.yaml`):
- Console and file logging
- DEBUG level for com.techtune.nexus
- SQL logging enabled (hibernate.show_sql: false, but type descriptor TRACE)
- Log file: `logs/nexus-core.log`

## Metrics Reference

### Job Metrics

| Metric Name | Type | Description |
|-------------|------|-------------|
| nexus.jobs.submitted | Counter | Total jobs submitted |
| nexus.jobs.completed | Counter | Jobs completed successfully |
| nexus.jobs.failed | Counter | Jobs permanently failed |
| nexus.jobs.retried | Counter | Job retries |
| nexus.jobs.cancelled | Counter | Jobs cancelled |
| nexus.jobs.lease_recoveries | Counter | Lease recovery events |
| nexus.jobs.execution_duration | Timer | Job execution duration |

### Webhook Metrics

| Metric Name | Type | Description |
|-------------|------|-------------|
| nexus.webhook.deliveries.attempted | Counter | Delivery attempts |
| nexus.webhook.deliveries.successful | Counter | Successful deliveries |
| nexus.webhook.deliveries.failed | Counter | Failed deliveries |
| nexus.webhook.deliveries.retried | Counter | Retries |
| nexus.webhook.deliveries.dead_lettered | Counter | Dead-letter transitions |
| nexus.webhook.deliveries.lease_recovered | Counter | Lease recoveries |
| nexus.webhook.delivery.duration | Timer | Delivery latency |

## Operational Runbooks

### Checking Application Health

```bash
curl http://localhost:8080/actuator/health
```

### Viewing Metrics

```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/nexus.jobs.submitted
curl http://localhost:8080/actuator/prometheus
```

### Investigating Jobs That Remain Pending

1. Check worker status via metrics
2. Verify worker is enabled: `nexus.worker.enabled=true`
3. Check job status via API
4. Review logs for worker errors
5. Verify database connectivity

### Investigating Repeated Job Failures

1. Check job status via API
2. Review job error messages
3. Check handler registration
4. Verify job type is registered
5. Review execution attempts

### Investigating Webhook Delivery Failures

1. Check webhook delivery metrics
2. Review dead-letter entries via API
3. Verify webhook endpoint configuration
4. Check SSRF protection logs
5. Test webhook endpoint connectivity

### Inspecting Dead-Lettered Webhook Deliveries

```bash
curl -H "X-API-Key: <key>" http://localhost:8080/api/webhook/deliveries?status=DEAD_LETTERED
```

### Replaying Dead-Lettered Webhook Deliveries

```bash
curl -X POST -H "X-API-Key: <key>" http://localhost:8080/api/webhook/deliveries/{id}/replay
```

### Understanding Metrics

- Counters: Cumulative values that only increase
- Timers: Duration measurements with statistics (mean, max, p95, p99)
- View Prometheus metrics for detailed time-series data

### Handling Database Connectivity Failures

1. Check `/actuator/health` for database status
2. Verify DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD
3. Check PostgreSQL container is running
4. Review HikariCP connection pool logs
5. Verify network connectivity to database

### Performing a Controlled Restart

```bash
# Graceful shutdown (SIGTERM)
curl -X POST http://localhost:8080/actuator/shutdown

# Or stop the process
kill <pid>
```

The application uses `server.shutdown: graceful` in configuration.

## Limitations

1. **No Custom Health Indicators**: Spring Boot Actuator uses default health indicators only
2. **No Worker/Scheduler Health Indicators**: Health indicators for worker and scheduler were attempted but removed due to dependency conflicts
3. **No Performance Tests**: Performance benchmarks not implemented (requires external tooling)
4. **No Security Audit**: Formal security review not performed in this phase
5. **No Alerting**: Alerting system not configured (requires external monitoring platform)
6. **In-Memory Rate Limiting**: Rate limiting is per-instance, not cluster-wide
7. **No Distributed Tracing**: Request tracing not implemented
8. **No Structured Logging**: Standard logging without structured fields

## Deployment Requirements

1. **Metrics Export**: Prometheus endpoint exposed - configure Prometheus to scrape
2. **Log Aggregation**: Configure log shipping to centralized log system
3. **Monitoring**: Set up dashboards for job and webhook metrics
4. **Alerting**: Configure alerts for failed jobs, webhook delivery failures, and database issues
5. **Rate Limiting**: For multi-instance deployments, implement distributed rate limiting (Redis)

## Next Steps

Phase 7 will implement the Next.js dashboard, which will:
- Visualize job and webhook metrics
- Display execution statistics
- Provide job management UI
- Show webhook delivery status
- Enable operational monitoring

## Test Results

- **Total Tests**: 124
- **Passed**: 124
- **Failed**: 0
- **Errors**: 0
- **Skipped**: 1
- **Build**: SUCCESS

All existing Phase 0-5 functionality remains intact. No regressions detected.
