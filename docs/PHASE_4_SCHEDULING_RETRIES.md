# Phase 4: Scheduling and Retries

## Overview
Phase 4 implements retry policies, delayed job execution, and recurring schedules with cron-based scheduling.

## Components Implemented

### 1. Retry Policy Engine
**File**: `RetryPolicy.java`

Features:
- **Fixed backoff**: Constant delay between retries
- **Exponential backoff**: Delay doubles with each retry (5s, 10s, 20s, 40s, ...)
- **Configurable parameters**:
  - `initialDelay`: Starting delay duration
  - `maxDelay`: Maximum delay cap
  - `maxAttempts`: Maximum number of retry attempts
  - `jitterFactor`: Random delay variation (0.0 to 1.0)
- **Validation**: Validates configuration on construction
- **Default policy**: Exponential backoff, 5s initial, 5min max, 5 attempts, 10% jitter

### 2. Delayed Job Execution
**Implementation**: Existing `Job.scheduledAt` field is now fully supported.

The worker engine (from Phase 3) respects `scheduledAt`:
- Jobs with `scheduledAt <= now` are claimable
- Jobs with `scheduledAt > now` are not claimed yet
- Delayed execution is enforced at the database level in the claiming query

### 3. Persistent Recurring Schedules
**Files**:
- `Schedule.java`: Schedule entity
- `ScheduleRepository.java`: Repository with due schedule queries
- `V4__add_schedules.sql`: Database migration

Schedule features:
- Tenant ownership
- Job type and payload
- Cron expression (Unix 5-field syntax)
- Timezone support (IANA timezone IDs)
- Next execution time tracking
- Enable/disable status
- Job configuration (priority, max attempts, timeout)

### 4. Cron Evaluation
**File**: `CronEvaluator.java`

Features:
- Validates cron expressions
- Calculates next execution time
- Timezone-aware evaluation
- Uses cron-utils library with Unix cron syntax
- Returns Optional<Instant> for safe handling

### 5. Schedule Management API
**Files**:
- `ScheduleController.java`: REST endpoints
- `ScheduleService.java`: Application service
- `CreateScheduleRequest.java`: Request DTO
- `ScheduleResponse.java`: Response DTO

Endpoints:
- `POST /api/v1/schedules` - Create schedule (requires `schedules:write`)
- `GET /api/v1/schedules/{id}` - Get schedule (requires `schedules:read`)
- `GET /api/v1/schedules` - List schedules (requires `schedules:read`)
- `PUT /api/v1/schedules/{id}` - Update schedule (requires `schedules:write`)
- `POST /api/v1/schedules/{id}/pause` - Pause schedule (requires `schedules:write`)
- `POST /api/v1/schedules/{id}/resume` - Resume schedule (requires `schedules:write`)
- `DELETE /api/v1/schedules/{id}` - Delete schedule (requires `schedules:write`)

### 6. Scheduler Lifecycle
**File**: `Scheduler.java`

Features:
- Background scheduler using Spring `@Scheduled`
- Polls for due schedules at configurable interval (default: 60s)
- Generates job instances from due schedules
- Advances schedule to next execution time
- Transactional processing to prevent duplicates
- Conditional on `nexus.scheduler.enabled` property
- Graceful error handling

Configuration:
```yaml
nexus:
  scheduler:
    enabled: true
    polling-interval: 60s
```

### 7. Database Migration
**File**: `V4__add_schedules.sql`

Creates:
- `schedules` table with constraints and indexes
- Foreign key to tenants with cascade delete
- Indexes for due schedule queries
- Check constraints for priority and max attempts

## Configuration

### Retry Policy
Retry policies are stored as JSON in the `jobs.retry_policy` column. Example:
```json
{
  "backoffStrategy": "EXPONENTIAL",
  "initialDelay": "PT5S",
  "maxDelay": "PT5M",
  "maxAttempts": 5,
  "jitterFactor": 0.1
}
```

### Schedule Example
```json
{
  "tenantId": "uuid",
  "jobType": "email.send",
  "payload": {"to": "user@example.com"},
  "cronExpression": "0 9 * * *",
  "timezone": "America/New_York",
  "jobPriority": 5,
  "jobMaxAttempts": 3
}
```

## Test Results
- Unit tests: 62 tests, 0 failures, 0 errors, 1 skipped
- Retry policy tests: 11 passing
- Cron evaluator tests: 6 passing
- Integration tests: Including worker concurrency tests from Phase 3

## Known Limitations
1. **Missed occurrences**: Scheduler does not generate backlog after prolonged downtime. On restart, it schedules the next occurrence only.
2. **No missed occurrence tracking**: Schedule does not track when jobs were skipped.
3. **Cron syntax**: Only Unix 5-field syntax is supported (no seconds field).
4. **No schedule limits**: No limit on number of jobs generated per schedule in a given period.
5. **No deduplication**: Scheduler relies on database constraints; duplicate job generation is prevented by transactional claiming but not at the schedule level.

## API Scope Requirements
The schedule endpoints require new scopes:
- `schedules:read` - For GET endpoints
- `schedules:write` - For POST, PUT, DELETE, pause, resume

These scopes should be added to API keys for schedule management.

## Dependencies Added
- `cron-utils:9.2.1` - Cron expression parsing and evaluation

## Future Enhancements
- Add missed occurrence handling
- Add schedule limits (max jobs per day/week)
- Add schedule history
- Add schedule execution statistics
- Add validation for job type registration at schedule creation
