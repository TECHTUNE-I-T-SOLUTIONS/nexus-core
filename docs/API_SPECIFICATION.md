# NEXUS CORE - API Specification

## API Conventions

### Base URL

All API endpoints are prefixed with `/api/v1`:

```
https://api.nexus-core.example.com/api/v1
```

### Authentication

Authentication is performed using API keys via the `Authorization` header:

```
Authorization: Bearer nx_live_abc123...
```

API key format:
- Prefix: `nx_live_` for production keys, `nx_test_` for test keys
- Random string (cryptographically secure)
- Total length: ~40 characters

### Versioning

API versioning is through URL path:
- Current version: `v1`
- Breaking changes will increment the version
- Non-breaking changes maintain the same version

### Response Format

**Success Response:**
```json
{
  "data": { ... },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

**Error Response:**
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameter",
    "details": { ... }
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### HTTP Status Codes

| Code | Usage |
|------|-------|
| 200 | Success (GET, PATCH) |
| 201 | Created (POST) |
| 204 | No Content (DELETE) |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (invalid/missing API key) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 409 | Conflict (duplicate idempotency key) |
| 422 | Unprocessable Entity (business logic error) |
| 429 | Too Many Requests (rate limit exceeded) |
| 500 | Internal Server Error |
| 503 | Service Unavailable |

### Pagination

List endpoints support pagination via query parameters:

```
GET /api/v1/jobs?page=1&limit=50&sort=created_at&order=desc
```

**Response:**
```json
{
  "data": [ ... ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 1000,
    "totalPages": 20
  }
}
```

### Filtering

List endpoints support filtering via query parameters:

```
GET /api/v1/jobs?status=RUNNING&job_type=email.send
```

### Idempotency

Idempotency is supported via the `Idempotency-Key` header:

```
POST /api/v1/jobs
Idempotency-Key: unique-key-per-tenant
```

If the same idempotency key is used, the original response is returned.

## Jobs API

### Submit Job

Create a new background job.

**Endpoint:**
```
POST /api/v1/jobs
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
Idempotency-Key: optional-unique-key
Content-Type: application/json
```

**Request Body:**
```json
{
  "jobType": "email.send",
  "payload": {
    "to": "user@example.com",
    "subject": "Welcome",
    "body": "Hello!"
  },
  "priority": 5,
  "scheduledAt": "2024-01-01T12:00:00Z",
  "maxAttempts": 3,
  "retryPolicy": {
    "strategy": "exponential",
    "initialDelaySeconds": 60,
    "multiplier": 2.0,
    "maxDelaySeconds": 3600
  },
  "timeoutSeconds": 300
}
```

**Field Descriptions:**
- `jobType` (required) - Registered handler type
- `payload` (required) - JSON payload for the handler
- `priority` (optional) - 0-10, default 5 (higher = higher priority)
- `scheduledAt` (optional) - ISO 8601 timestamp, default now
- `maxAttempts` (optional) - Maximum retry attempts, default 3
- `retryPolicy` (optional) - Retry configuration
- `timeoutSeconds` (optional) - Execution timeout, default 300

**Response (201 Created):**
```json
{
  "data": {
    "jobId": "uuid",
    "status": "PENDING",
    "createdAt": "2024-01-01T00:00:00Z",
    "scheduledAt": "2024-01-01T00:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

**Error Responses:**
- 400 - Invalid request (validation error)
- 401 - Unauthorized
- 409 - Duplicate idempotency key
- 422 - Invalid job type or business logic error

### List Jobs

Retrieve a paginated list of jobs.

**Endpoint:**
```
GET /api/v1/jobs
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Query Parameters:**
- `page` (optional) - Page number, default 1
- `limit` (optional) - Items per page, default 50, max 100
- `status` (optional) - Filter by status
- `jobType` (optional) - Filter by job type
- `sort` (optional) - Sort field, default `createdAt`
- `order` (optional) - Sort order, `asc` or `desc`, default `desc`

**Response (200 OK):**
```json
{
  "data": [
    {
      "jobId": "uuid",
      "jobType": "email.send",
      "status": "SUCCEEDED",
      "priority": 5,
      "createdAt": "2024-01-01T00:00:00Z",
      "scheduledAt": "2024-01-01T00:00:00Z",
      "completedAt": "2024-01-01T00:01:00Z",
      "attemptCount": 1
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 1000,
    "totalPages": 20
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Get Job

Retrieve details of a specific job.

**Endpoint:**
```
GET /api/v1/jobs/{jobId}
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Response (200 OK):**
```json
{
  "data": {
    "jobId": "uuid",
    "jobType": "email.send",
    "payload": { ... },
    "priority": 5,
    "status": "SUCCEEDED",
    "createdAt": "2024-01-01T00:00:00Z",
    "scheduledAt": "2024-01-01T00:00:00Z",
    "attemptCount": 1,
    "maxAttempts": 3,
    "retryPolicy": { ... },
    "timeoutSeconds": 300,
    "workerId": "worker-123",
    "claimedAt": "2024-01-01T00:00:00Z",
    "completedAt": "2024-01-01T00:01:00Z",
    "lastExecutionStartedAt": "2024-01-01T00:00:00Z",
    "lastExecutionCompletedAt": "2024-01-01T00:01:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

**Error Responses:**
- 401 - Unauthorized
- 403 - Forbidden (not your job)
- 404 - Job not found

### Cancel Job

Cancel a job that hasn't completed.

**Endpoint:**
```
POST /api/v1/jobs/{jobId}/cancel
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Response (200 OK):**
```json
{
  "data": {
    "jobId": "uuid",
    "status": "CANCELLED",
    "cancelledAt": "2024-01-01T00:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

**Error Responses:**
- 400 - Job cannot be cancelled (already completed)
- 401 - Unauthorized
- 403 - Forbidden (not your job)
- 404 - Job not found

### Retry Job

Retry a failed job.

**Endpoint:**
```
POST /api/v1/jobs/{jobId}/retry
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Request Body (optional):**
```json
{
  "scheduledAt": "2024-01-01T12:00:00Z"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "jobId": "uuid",
    "status": "QUEUED",
    "scheduledAt": "2024-01-01T12:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

**Error Responses:**
- 400 - Job cannot be retried (not failed or dead-lettered)
- 401 - Unauthorized
- 403 - Forbidden (not your job)
- 404 - Job not found

### Get Job Execution History

Retrieve execution attempt history for a job.

**Endpoint:**
```
GET /api/v1/jobs/{jobId}/attempts
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Response (200 OK):**
```json
{
  "data": [
    {
      "attemptId": "uuid",
      "attemptNumber": 1,
      "workerId": "worker-123",
      "startedAt": "2024-01-01T00:00:00Z",
      "completedAt": "2024-01-01T00:01:00Z",
      "status": "FAILED",
      "errorMessage": "Connection timeout",
      "errorDetails": { ... }
    }
  ],
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

## Schedules API

### Create Schedule

Create a recurring schedule.

**Endpoint:**
```
POST /api/v1/schedules
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Daily Report",
  "jobType": "report.generate",
  "payload": {
    "type": "daily"
  },
  "cronExpression": "0 9 * * *",
  "timezone": "America/New_York"
}
```

**Field Descriptions:**
- `name` (required) - Human-readable schedule name
- `jobType` (required) - Registered handler type
- `payload` (required) - JSON payload for each job instance
- `cronExpression` (required) - Cron expression (5-field)
- `timezone` (optional) - IANA timezone, default UTC

**Response (201 Created):**
```json
{
  "data": {
    "scheduleId": "uuid",
    "name": "Daily Report",
    "status": "ACTIVE",
    "nextRunAt": "2024-01-01T09:00:00Z",
    "createdAt": "2024-01-01T00:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### List Schedules

Retrieve a paginated list of schedules.

**Endpoint:**
```
GET /api/v1/schedules
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Query Parameters:**
- `page` (optional) - Page number, default 1
- `limit` (optional) - Items per page, default 50
- `status` (optional) - Filter by status

**Response (200 OK):**
```json
{
  "data": [
    {
      "scheduleId": "uuid",
      "name": "Daily Report",
      "jobType": "report.generate",
      "cronExpression": "0 9 * * *",
      "timezone": "America/New_York",
      "status": "ACTIVE",
      "nextRunAt": "2024-01-01T09:00:00Z",
      "lastRunAt": "2023-12-31T09:00:00Z",
      "createdAt": "2023-01-01T00:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 10,
    "totalPages": 1
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Get Schedule

Retrieve details of a specific schedule.

**Endpoint:**
```
GET /api/v1/schedules/{scheduleId}
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Response (200 OK):**
```json
{
  "data": {
    "scheduleId": "uuid",
    "name": "Daily Report",
    "jobType": "report.generate",
    "payload": { ... },
    "cronExpression": "0 9 * * *",
    "timezone": "America/New_York",
    "status": "ACTIVE",
    "nextRunAt": "2024-01-01T09:00:00Z",
    "lastRunAt": "2023-12-31T09:00:00Z",
    "createdAt": "2023-01-01T00:00:00Z",
    "updatedAt": "2023-12-01T00:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Update Schedule

Update a schedule.

**Endpoint:**
```
PATCH /api/v1/schedules/{scheduleId}
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
Content-Type: application/json
```

**Request Body (partial update):**
```json
{
  "status": "PAUSED",
  "cronExpression": "0 10 * * *"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "scheduleId": "uuid",
    "status": "PAUSED",
    "updatedAt": "2024-01-01T00:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Delete Schedule

Delete a schedule.

**Endpoint:**
```
DELETE /api/v1/schedules/{scheduleId}
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Response (204 No Content)**

## Webhooks API

### Create Webhook

Create a webhook endpoint.

**Endpoint:**
```
POST /api/v1/webhooks
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Job Status Webhook",
  "url": "https://example.com/webhooks/jobs",
  "events": ["job.started", "job.succeeded", "job.failed"],
  "secret": "webhook-secret-key"
}
```

**Field Descriptions:**
- `name` (required) - Human-readable webhook name
- `url` (required) - HTTPS endpoint URL
- `events` (required) - Array of event types to deliver
- `secret` (optional) - Secret for request signing

**Response (201 Created):**
```json
{
  "data": {
    "webhookId": "uuid",
    "name": "Job Status Webhook",
    "url": "https://example.com/webhooks/jobs",
    "events": ["job.started", "job.succeeded", "job.failed"],
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### List Webhooks

Retrieve a paginated list of webhooks.

**Endpoint:**
```
GET /api/v1/webhooks
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Response (200 OK):**
```json
{
  "data": [
    {
      "webhookId": "uuid",
      "name": "Job Status Webhook",
      "url": "https://example.com/webhooks/jobs",
      "events": ["job.started", "job.succeeded", "job.failed"],
      "status": "ACTIVE",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 5,
    "totalPages": 1
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Get Webhook

Retrieve details of a specific webhook.

**Endpoint:**
```
GET /api/v1/webhooks/{webhookId}
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Response (200 OK):**
```json
{
  "data": {
    "webhookId": "uuid",
    "name": "Job Status Webhook",
    "url": "https://example.com/webhooks/jobs",
    "events": ["job.started", "job.succeeded", "job.failed"],
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Update Webhook

Update a webhook.

**Endpoint:**
```
PATCH /api/v1/webhooks/{webhookId}
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
Content-Type: application/json
```

**Request Body (partial update):**
```json
{
  "status": "INACTIVE",
  "url": "https://example.com/webhooks/jobs-v2"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "webhookId": "uuid",
    "status": "INACTIVE",
    "updatedAt": "2024-01-01T00:00:00Z"
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Delete Webhook

Delete a webhook.

**Endpoint:**
```
DELETE /api/v1/webhooks/{webhookId}
```

**Headers:**
```
Authorization: Bearer nx_live_abc123...
```

**Response (204 No Content)**

## Error Handling

### Error Codes

| Code | Description |
|------|-------------|
| `VALIDATION_ERROR` | Request validation failed |
| `AUTHENTICATION_ERROR` | Invalid or missing API key |
| `AUTHORIZATION_ERROR` | Insufficient permissions |
| `NOT_FOUND` | Resource not found |
| `CONFLICT` | Resource conflict (duplicate) |
| `RATE_LIMIT_EXCEEDED` | Rate limit exceeded |
| `INTERNAL_ERROR` | Internal server error |
| `SERVICE_UNAVAILABLE` | Service temporarily unavailable |

### Error Response Structure

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameter",
    "details": {
      "field": "jobType",
      "reason": "Job type is required"
    }
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Rate Limiting

Rate limits are enforced per tenant:
- Default: 100 requests per minute
- Headers included in responses:
  - `X-RateLimit-Limit` - Request limit
  - `X-RateLimit-Remaining` - Remaining requests
  - `X-RateLimit-Reset` - Reset timestamp

On rate limit exceeded (429):
```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded",
    "details": {
      "retryAfter": 60
    }
  }
}
```

## Event Types

### Job Events

Webhook events for job lifecycle:

- `job.accepted` - Job submitted and accepted
- `job.started` - Job execution started
- `job.succeeded` - Job completed successfully
- `job.failed` - Job failed
- `job.dead_lettered` - Job permanently failed
- `job.cancelled` - Job was cancelled

### Event Payload Example

```json
{
  "eventId": "uuid",
  "eventType": "job.succeeded",
  "tenantId": "uuid",
  "timestamp": "2024-01-01T00:00:00Z",
  "data": {
    "jobId": "uuid",
    "jobType": "email.send",
    "status": "SUCCEEDED",
    "attemptCount": 1,
    "completedAt": "2024-01-01T00:01:00Z"
  }
}
```

## Webhook Signature

Webhook requests are signed using HMAC-SHA256:

```
X-Signature: sha256=<signature>
```

Signature calculation:
```
signature = hmac_sha256(secret, payload)
```

Verification:
```java
String receivedSignature = request.getHeader("X-Signature");
String calculatedSignature = "sha256=" + hmacSha256(secret, payload);
if (!MessageDigest.isEqual(receivedSignature, calculatedSignature)) {
    // Reject request
}
```
