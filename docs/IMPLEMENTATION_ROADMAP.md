# NEXUS CORE - Implementation Roadmap

## Overview

This roadmap breaks down the development of NEXUS CORE into logical phases. Each phase has defined scope, dependencies, deliverables, acceptance criteria, and testing requirements.

## Phase 0: Repository and Architecture Foundation

### Scope
- Repository inspection and verification
- Build system validation
- Documentation foundation
- Environment configuration
- Database infrastructure setup

### Dependencies
- None (starting point)

### Deliverables
- Verified Maven build
- Comprehensive documentation in `/docs`
- Configured `application.yml`
- Environment variable setup
- Docker Compose for PostgreSQL
- `.gitignore` configuration
- `.env.example` file

### Acceptance Criteria
- [ ] Maven build completes successfully
- [ ] All documentation files created and reviewed
- [ ] Application can connect to PostgreSQL via Docker Compose
- [ ] Flyway migrations directory structure created
- [ ] Environment variables documented
- [ ] Git repository is clean and properly configured

### Testing Requirements
- Verify Maven build: `./mvnw clean verify`
- Test database connectivity
- Verify Flyway can run migrations

### Risks
- Spring Boot 4.1.2 dependency compatibility
- Maven dependency resolution issues
- Docker networking issues on Windows

---

## Phase 1: Core Domain and Persistence

### Scope
- Tenant domain model
- Job domain model
- Job state machine implementation
- Initial database schema
- Repository layer
- DTOs and validation
- Standard error handling

### Dependencies
- Phase 0 complete
- PostgreSQL running

### Deliverables
- Tenant entity and repository
- Job entity and repository
- Job state machine service
- Database migration V1__initial_schema.sql
- DTOs for requests/responses
- Validation annotations
- Global exception handler
- Unit tests for domain logic
- Integration tests for repositories

### Acceptance Criteria
- [ ] Tenant can be created and retrieved
- [ ] Job can be created with all required fields
- [ ] Job state transitions are validated
- [ ] Database schema matches design document
- [ ] Flyway migrations run successfully
- [ ] All unit tests pass
- [ ] All integration tests pass with Testcontainers

### Testing Requirements
**Unit Tests:**
- Job state transition validation
- Tenant entity constraints
- DTO validation
- Error handling

**Integration Tests:**
- Repository CRUD operations
- Database constraints
- Flyway migrations
- Transaction behavior

### Risks
- JPA entity mapping issues
- Flyway migration conflicts
- State machine complexity

---

## Phase 2: API and Authentication

### Scope
- API key generation and hashing
- Tenant authentication filter
- Job submission endpoint
- Job retrieval endpoints
- Job listing with pagination
- Cancellation and retry requests
- Tenant isolation enforcement

### Dependencies
- Phase 1 complete

### Deliverables
- API key entity and repository
- API key generation service
- Authentication filter
- Security context propagation
- Job submission controller
- Job retrieval controller
- Job listing controller
- Cancellation endpoint
- Retry endpoint
- Pagination support
- API documentation (OpenAPI/Swagger)

### Acceptance Criteria
- [ ] API keys can be generated and validated
- [ ] Authentication is enforced on all endpoints
- [ ] Tenant context is properly propagated
- [ ] Jobs can be submitted via API
- [ ] Jobs can be retrieved by ID
- [ ] Jobs can be listed with pagination
- [ ] Jobs can be cancelled
- [ ] Failed jobs can be retried
- [ ] Tenant isolation is enforced (cross-tenant access blocked)
- [ ] API documentation is available

### Testing Requirements
**Unit Tests:**
- API key hashing and validation
- Authentication logic
- Controller validation
- Pagination logic

**Integration Tests:**
- API authentication
- Job submission and retrieval
- Tenant isolation
- Error responses
- Pagination

**API Tests:**
- Authentication with valid/invalid keys
- Request validation
- Authorization checks
- Error handling

### Risks
- Security context propagation issues
- Authentication filter timing
- Pagination edge cases
- Tenant isolation bypass

---

## Phase 3: Execution Engine

### Scope
- Handler registry and allowlisting
- Worker lifecycle management
- Job claiming with database locking
- Concurrency protection
- Lease management and renewal
- Execution tracking
- Worker recovery mechanism

### Dependencies
- Phase 2 complete

### Deliverables
- Handler registry interface
- Sample handler implementations
- Worker service (polling engine)
- Job claimer with `FOR UPDATE SKIP LOCKED`
- Lease manager
- Execution tracker
- Execution attempt tracking
- Worker recovery logic
- Unit tests for execution logic
- Integration tests for worker behavior

### Acceptance Criteria
- [ ] Handlers can be registered and retrieved
- [ ] Unknown job types are rejected
- [ ] Worker polls for eligible jobs
- [ ] Job claiming uses row-level locking
- [ ] Multiple workers can run without conflicts
- [ ] Worker renews leases for long-running jobs
- [ ] Expired leases are recovered
- [ ] Execution attempts are tracked
- [ ] Worker crashes are handled gracefully

### Testing Requirements
**Unit Tests:**
- Handler registry logic
- Lease calculation
- Retry policy calculation
- State machine transitions

**Integration Tests:**
- Worker polling and claiming
- Concurrent worker operation
- Lease expiration and recovery
- Execution tracking
- Worker crash simulation

**Reliability Tests:**
- Multiple workers claiming jobs
- Worker crash during execution
- Lease expiration scenarios
- Concurrent modification

### Risks
- Database locking issues
- Lease renewal race conditions
- Worker shutdown handling
- Handler execution errors

---

## Phase 4: Scheduling and Retries

### Scope
- Delayed job execution
- Retry policy engine
- Backoff strategies (fixed, exponential)
- Cron expression parsing
- Recurring schedule management
- Schedule generation
- Timezone handling
- Schedule persistence

### Dependencies
- Phase 3 complete

### Deliverables
- Retry policy engine
- Backoff calculator
- Cron expression parser
- Schedule entity and repository
- Schedule generator
- Scheduler service
- Timezone-aware scheduling
- Recurring schedule management
- Unit tests for scheduling logic
- Integration tests for scheduler

### Acceptance Criteria
- [ ] Jobs can be scheduled for future execution
- [ ] Retry policies are applied correctly
- [ ] Fixed delay backoff works
- [ ] Exponential backoff works
- [ ] Jitter is applied when configured
- [ ] Cron expressions are parsed correctly
- [ ] Recurring schedules create job instances
- [ ] Timezone handling is correct
- [ ] Daylight saving transitions are handled

### Testing Requirements
**Unit Tests:**
- Retry policy calculations
- Backoff calculations
- Cron expression parsing
- Schedule generation
- Timezone conversions

**Integration Tests:**
- Delayed job execution
- Retry scheduling
- Recurring schedule execution
- Timezone scenarios
- Daylight saving transitions

**Reliability Tests:**
- Schedule consistency across timezones
- Retry exhaustion
- Long-running schedules

### Risks
- Cron expression parsing errors
- Timezone handling complexity
- Daylight saving edge cases
- Schedule drift over time

---

## Phase 5: Webhooks and Event Delivery

### Scope
- Webhook endpoint registration
- Event generation for job lifecycle
- Transactional outbox pattern
- Webhook request signing
- Outbound delivery service
- Delivery retry logic
- Delivery history tracking

### Dependencies
- Phase 4 complete

### Deliverables
- Webhook entity and repository
- Event publisher
- Outbox event entity and repository
- Transactional outbox writer
- Webhook delivery service
- Signature generator
- Delivery retry logic
- Delivery attempt tracking
- Unit tests for webhook logic
- Integration tests for delivery

### Acceptance Criteria
- [ ] Webhook endpoints can be registered
- [ ] Events are generated for job lifecycle changes
- [ ] Events are written to outbox transactionally
- [ ] Outbox events are processed reliably
- [ ] Webhook requests are signed
- [ ] Delivery retries use exponential backoff
- [ ] Delivery history is tracked
- [ ] Failed webhooks don't block job execution

### Testing Requirements
**Unit Tests:**
- Event generation logic
- Signature calculation
- Retry calculation
- Outbox transaction logic

**Integration Tests:**
- Webhook registration
- Event generation and delivery
- Transactional outbox
- Delivery retries
- Signature verification

**Reliability Tests:**
- Webhook endpoint failures
- Network timeouts
- Duplicate event prevention
- Outbox event consistency

### Risks
- Outbox event loss
- Webhook delivery blocking job execution
- Signature verification failures
- Event ordering issues

---

## Phase 6: Observability and Operational Readiness

### Scope
- Health checks
- Custom metrics
- Structured logging
- Execution statistics
- Worker monitoring
- Operational endpoints
- Performance testing
- Security review

### Dependencies
- Phase 5 complete

### Deliverables
- Custom health indicators
- Metrics for job processing
- Structured logging configuration
- Execution statistics service
- Worker monitoring endpoints
- Operational tools
- Performance test suite
- Security audit checklist

### Acceptance Criteria
- [ ] Health checks report system status
- [ ] Metrics are exposed for job processing
- [ ] Logs are structured and searchable
- [ ] Execution statistics are available
- [ ] Worker status can be monitored
- [ ] Performance meets baseline requirements
- [ ] Security review is complete
- [ ] Operational runbooks are documented

### Testing Requirements
**Unit Tests:**
- Health indicator logic
- Metrics calculation
- Statistics aggregation

**Integration Tests:**
- Health check endpoints
- Metrics endpoints
- Logging output

**Performance Tests:**
- Load testing with concurrent job submission
- Worker throughput testing
- Database query performance
- API response time testing

**Security Tests:**
- Authentication bypass attempts
- Authorization bypass attempts
- SQL injection attempts
- XSS prevention

### Risks
- Performance bottlenecks
- Monitoring gaps
- Security vulnerabilities
- Log volume management

---

## Phase 7: Dashboard

### Scope
- Next.js application setup
- Authentication integration
- Tenant overview page
- Job monitoring
- Job details view
- Execution history
- Schedule management
- Webhook management
- Usage statistics

### Dependencies
- Phase 6 complete

### Deliverables
- Next.js application
- Authentication integration
- Tenant overview page
- Job list and search
- Job details page
- Execution history view
- Schedule management UI
- Webhook management UI
- Usage statistics dashboard

### Acceptance Criteria
- [ ] Dashboard can authenticate users
- [ ] Tenant overview shows key metrics
- [ ] Jobs can be listed and searched
- [ ] Job details show complete information
- [ ] Execution history is visible
- [ ] Schedules can be created and managed
- [ ] Webhooks can be created and managed
- [ ] Usage statistics are displayed

### Testing Requirements
**Unit Tests:**
- Component rendering
- API integration
- State management

**Integration Tests:**
- Authentication flow
- API communication
- User interactions

**E2E Tests:**
- Complete user workflows
- Error handling
- Responsive design

### Risks
- API integration complexity
- State management complexity
- Performance with large datasets
- Browser compatibility

---

## Phase 8: Production Readiness

### Scope
- Deployment architecture
- Configuration hardening
- Database backup strategy
- Monitoring and alerting
- Load testing
- Security testing
- Documentation
- Operational runbooks

### Dependencies
- Phase 7 complete

### Deliverables
- Deployment architecture documentation
- Production configuration templates
- Database backup procedures
- Monitoring and alerting setup
- Load test results
- Security audit report
- Complete documentation
- Operational runbooks
- Disaster recovery plan

### Acceptance Criteria
- [ ] Deployment architecture is documented
- [ ] Production configuration is hardened
- [ ] Database backups are automated
- [ ] Monitoring and alerting are configured
- [ ] Load testing meets requirements
- [ ] Security audit passes
- [ ] Documentation is complete
- [ ] Runbooks are tested
- [ ] Disaster recovery is tested

### Testing Requirements
**Load Testing:**
- Simulated production workload
- Peak load testing
- Sustained load testing
- Failure scenario testing

**Security Testing:**
- Penetration testing
- Dependency vulnerability scanning
- Configuration audit
- Access control review

**Disaster Recovery Testing:**
- Database restore
- Application failover
- Data consistency verification

### Risks
- Deployment complexity
- Configuration errors
- Backup failures
- Monitoring gaps
- Security vulnerabilities

---

## Milestone Criteria

### M1: Foundation Complete
- Phase 0 and Phase 1 complete
- Database schema finalized
- Domain models implemented
- Tests passing

### M2: API Functional
- Phase 2 complete
- Jobs can be submitted and managed via API
- Authentication working
- Tenant isolation enforced

### M3: Execution Working
- Phase 3 complete
- Workers can execute jobs
- Job claiming working
- Basic reliability demonstrated

### M4: Scheduling and Retries
- Phase 4 complete
- Delayed execution working
- Retries working
- Recurring schedules working

### M5: Event Delivery
- Phase 5 complete
- Webhooks working
- Event delivery reliable
- Outbox pattern implemented

### M6: Observable
- Phase 6 complete
- Monitoring in place
- Metrics available
- Performance validated

### M7: User Interface
- Phase 7 complete
- Dashboard functional
- Users can manage jobs
- Visual monitoring available

### M8: Production Ready
- Phase 8 complete
- Deployed to production
- Monitoring active
- Runbooks tested
- Documentation complete

---

## Parallel Work Opportunities

Some phases can have parallel work streams:

**Phase 2 + Phase 3:**
- API endpoints can be developed while worker engine is built
- Use test data or mock execution initially

**Phase 4 + Phase 5:**
- Scheduling can be developed while webhook infrastructure is built
- Independent modules

**Phase 6 + Phase 7:**
- Observability can be enhanced while dashboard is built
- Dashboard can use metrics from Phase 6

---

## Risk Mitigation

### Technical Risks
- **Dependency issues:** Test dependencies early, have fallback versions
- **Database performance:** Monitor query performance, add indexes as needed
- **Concurrency issues:** Test thoroughly with multiple workers
- **Security vulnerabilities:** Regular security audits, dependency scanning

### Project Risks
- **Scope creep:** Stick to defined phases, defer features
- **Timeline pressure:** Prioritize core functionality, defer nice-to-haves
- **Resource constraints:** Focus on highest-value features first

### Operational Risks
- **Deployment complexity:** Use containerization, automate deployments
- **Monitoring gaps:** Implement monitoring early, iterate
- **Documentation gaps:** Document as you build, not after

---

## Success Metrics

### Quality Metrics
- Test coverage > 80%
- Zero critical security vulnerabilities
- All acceptance criteria met per phase

### Performance Metrics
- API response time < 100ms (p95)
- Job submission throughput > 1000 jobs/second
- Worker throughput > 100 jobs/second per worker

### Reliability Metrics
- 99.9% uptime target
- < 0.1% job loss rate
- < 1 minute MTTR (Mean Time To Recovery)

### User Experience Metrics
- API documentation completeness
- Dashboard usability score
- Developer onboarding time < 1 hour
