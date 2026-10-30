# NEXUS CORE - Architecture Decisions

This document records important architectural decisions, alternatives considered, and the reasons for choosing a particular approach.

## Decision Log Format

Each decision follows this format:
- **Decision:** What was decided
- **Context:** Why the decision was needed
- **Alternatives:** What options were considered
- **Decision:** What was chosen and why
- **Consequences:** Impact of this decision
- **Status:** Accepted, Proposed, Deprecated, or Superseded

---

## ADR-001: Modular Monolith Architecture

**Context:**
NEXUS CORE needs to be a production-grade platform that can scale and evolve. We need to choose an architectural style that balances simplicity with future growth potential.

**Alternatives:**
1. **Microservices** - Independent services communicating via network
2. **Modular Monolith** - Single deployable unit with clear internal boundaries
3. **Distributed Monolith** - Monolithic deployment with distributed concerns

**Decision:**
Choose **Modular Monolith**.

**Reasoning:**
- Simpler deployment and operations initially
- Transaction boundaries are clear (single database)
- No distributed transaction complexity
- Lower infrastructure overhead
- Clear module boundaries allow future extraction if needed
- Faster development cycle
- Easier testing and debugging

**Consequences:**
- Single deployment unit for now
- Module boundaries must be respected to enable future extraction
- Database is shared (but can be partitioned later)
- Network communication is not needed between modules
- Future extraction requires careful planning

**Status:** Accepted

---

## ADR-002: PostgreSQL as Primary Database

**Context:**
NEXUS CORE needs a durable, reliable persistence layer for jobs, schedules, and metadata. We need to choose a database technology.

**Alternatives:**
1. **PostgreSQL** - Relational database with advanced features
2. **MySQL** - Relational database
3. **MongoDB** - NoSQL document database
4. **Redis** - In-memory data store (with persistence)
5. **Kafka** - Stream processing platform

**Decision:**
Choose **PostgreSQL**.

**Reasoning:**
- ACID transactions for reliability
- Row-level locking (`FOR UPDATE SKIP LOCKED`) for job claiming
- JSONB support for flexible payloads
- Mature ecosystem and tooling
- Strong data integrity through constraints
- Excellent performance for relational queries
- Good horizontal scaling with read replicas
- No need for additional infrastructure initially

**Consequences:**
- Relational data model (not purely document-based)
- SQL queries required
- Schema migrations needed (Flyway)
- Can scale with read replicas later
- May need to consider sharding at very large scale

**Status:** Accepted

---

## ADR-003: No Redis or Kafka Initially

**Context:**
Many job processing platforms use Redis for queues or Kafka for event streaming. We need to decide whether to include these from the start.

**Alternatives:**
1. **Include Redis** - For job queues and caching
2. **Include Kafka** - For event streaming
3. **Include both** - Maximum flexibility
4. **PostgreSQL only** - Simplify infrastructure

**Decision:**
Choose **PostgreSQL only** initially.

**Reasoning:**
- PostgreSQL provides sufficient coordination capabilities
- Row-level locking enables safe job claiming
- Reduces infrastructure complexity
- Lower operational overhead
- Faster development cycle
- Can add Redis/Kafka later if workload justifies it
- PostgreSQL is durable by default (no data loss risk)

**Consequences:**
- Polling-based worker (not push-based)
- Database is the coordination point
- May add Redis for caching if needed
- May add Kafka for event streaming if needed
- Simpler infrastructure to operate

**Status:** Accepted

---

## ADR-004: At-Least-Once Execution with Idempotency

**Context:**
Distributed systems cannot guarantee exactly-once execution. We need to define our execution guarantee.

**Alternatives:**
1. **Exactly-once** - Promise perfect execution (impossible in distributed systems)
2. **At-least-once** - Jobs may execute multiple times, provide idempotency
3. **At-most-once** - Jobs may be lost but never duplicated

**Decision:**
Choose **At-least-once with idempotency**.

**Reasoning:**
- Exactly-once is a false promise in distributed systems
- At-most-once risks data loss (unacceptable)
- At-least-once is achievable with reliable persistence
- Idempotency controls prevent side effects of duplicates
- Industry standard for job processing systems
- Honest communication with users about guarantees

**Consequences:**
- Jobs may execute multiple times in failure scenarios
- Handlers must be idempotent where duplicates are dangerous
- Idempotency keys available for duplicate prevention
- Users must design handlers to be idempotent
- Clear documentation about execution guarantees

**Status:** Accepted

---

## ADR-005: API Key Authentication

**Context:**
NEXUS CORE needs an authentication mechanism for tenant applications. We need to choose the initial authentication approach.

**Alternatives:**
1. **API Keys** - Simple token-based authentication
2. **OAuth2** - Industry-standard authorization framework
3. **JWT** - Self-contained tokens
4. **mTLS** - Mutual TLS authentication

**Decision:**
Choose **API Keys** initially.

**Reasoning:**
- Simple to implement and understand
- Sufficient for machine-to-machine communication
- Easy to rotate and revoke
- No external identity provider needed
- Can add OAuth2 later if enterprise customers require it
- Lower barrier to entry for developers

**Consequences:**
- Keys must be stored securely (hashed)
- Key rotation process needed
- No built-in user authentication (just machine auth)
- Can add OAuth2/JWT later without breaking changes
- Scope-based authorization model

**Status:** Accepted

---

## ADR-006: Handler Allowlisting

**Context:**
NEXUS CORE must execute jobs on behalf of users. We need to decide whether to allow arbitrary code execution or restrict to registered handlers.

**Alternatives:**
1. **Arbitrary code execution** - Users can execute any code
2. **Registered handlers only** - Allowlisted handler types
3. **Sandboxed execution** - Restricted code execution environment

**Decision:**
Choose **Registered handlers only**.

**Reasoning:**
- Security: Prevents code injection attacks
- Reliability: Only tested, known code runs
- Control: Platform controls what executes
- Compliance: Easier to audit and secure
- Resource management: Known resource usage patterns
- Industry standard for job platforms

**Consequences:**
- Users cannot execute arbitrary code
- Handlers must be registered at application startup
- New handler types require platform changes
- Clear security boundary
- May need to provide common handlers (email, HTTP, etc.)

**Status:** Accepted

---

## ADR-007: Docker Compose for Development

**Context:**
We need a development environment setup that is reproducible and simple to run.

**Alternatives:**
1. **Local installation** - Install PostgreSQL directly on developer machines
2. **Docker Compose** - Containerized PostgreSQL
3. **Kubernetes** - Full K8s setup for development
4. **Cloud database** - Use cloud-hosted PostgreSQL

**Decision:**
Choose **Docker Compose**.

**Reasoning:**
- Reproducible across machines
- Simple to start and stop
- Isolated from host system
- Easy to reset state
- Low overhead compared to Kubernetes
- No cloud costs for development
- Familiar to most developers

**Consequences:**
- Docker required on developer machines
- Windows may have networking considerations
- State is in containers (can be reset)
- Production deployment may differ (but that's expected)

**Status:** Accepted

---

## ADR-008: Row-Level Locking for Job Claiming

**Context:**
Multiple workers need to claim jobs without duplicates. We need a coordination mechanism.

**Alternatives:**
1. **Optimistic locking** - Version numbers, retry on conflict
2. **Pessimistic locking** - SELECT FOR UPDATE
3. **Redis distributed lock** - External lock service
4. **Queue-based** - Use a message queue

**Decision:**
Choose **PostgreSQL row-level locking with `FOR UPDATE SKIP LOCKED`**.

**Reasoning:**
- No additional infrastructure needed
- Atomic claiming within transaction
- SKIP LOCKED prevents lock contention
- Proven pattern for job processing
- Simple and reliable
- Database is already the source of truth

**Consequences:**
- Database handles coordination
- Workers poll for jobs (not push-based)
- Lock contention is minimal with SKIP LOCKED
- Need to handle lease expiration for worker crashes
- Database must be highly available

**Status:** Accepted

---

## ADR-009: Transactional Outbox for Webhooks

**Context:**
Webhook delivery must be reliable but shouldn't block job execution. We need a pattern for guaranteed event delivery.

**Alternatives:**
1. **Direct delivery** - Send webhook in same transaction
2. **Transactional outbox** - Write event to outbox, deliver separately
3. **Message queue** - Use Kafka/RabbitMQ for events
4. **Best-effort** - No guarantees, retry on failure

**Decision:**
Choose **Transactional outbox pattern**.

**Reasoning:**
- Guaranteed event persistence
- Doesn't block main transaction
- No additional infrastructure needed
- Reliable delivery with retries
- Audit trail of events
- Industry standard pattern

**Consequences:**
- Additional table for outbox events
- Separate process to deliver events
- Eventual consistency for webhooks
- Need to handle delivery failures
- Outbox cleanup required

**Status:** Accepted

---

## ADR-010: Spring Boot 4.1.2

**Context:**
The generated project uses Spring Boot 4.1.2. We need to decide whether to use this version or downgrade.

**Alternatives:**
1. **Use Spring Boot 4.1.2** - Current generated version
2. **Downgrade to 3.x** - More stable, longer support
3. **Upgrade to latest** - Cutting edge features

**Decision:**
Choose **Spring Boot 4.1.2** (keep generated version).

**Reasoning:**
- Generated project already uses this version
- Build verification shows it works
- Modern framework with current features
- Good Java 21 support
- No reason to change without issues
- Can downgrade if compatibility issues arise

**Consequences:**
- Using relatively new version
- May have fewer community resources than 3.x
- Modern features available
- If issues arise, consider downgrade to 3.2.x

**Status:** Accepted
**Note:** Monitor for compatibility issues, be prepared to downgrade to 3.2.x if needed.

---

## ADR-011: Package-by-Feature Structure

**Context:**
We need to organize code within the modular monolith. We need a package structure that supports clear boundaries.

**Alternatives:**
1. **Package-by-layer** - controllers, services, repositories packages
2. **Package-by-feature** - tenant, job, execution feature packages
3. **Hybrid** - Mix of both approaches

**Decision:**
Choose **Package-by-feature**.

**Reasoning:**
- Clear feature boundaries
- Easier to extract features to services later
- Related code is co-located
- Better for understanding feature scope
- Supports modular monolith goals
- Industry best practice for modular monoliths

**Consequences:**
- Code organization by business capability
- Each feature has its own layer packages
- Clearer separation of concerns
- Easier future service extraction

**Status:** Accepted

---

## ADR-012: UUIDs for Exposed Identifiers

**Context:**
Jobs, tenants, and other entities need identifiers. We need to decide between auto-increment integers and UUIDs.

**Alternatives:**
1. **Auto-increment integers** - Sequential numeric IDs
2. **UUIDs** - Universally unique identifiers
3. **ULIDs** - Ordered unique identifiers

**Decision:**
Choose **UUIDs** for externally exposed identifiers.

**Reasoning:**
- No information leakage (sequential IDs reveal usage)
- Safe for public URLs
- Can be generated client-side (for idempotency)
- No coordination needed for ID generation
- Standard practice for SaaS platforms
- Database can still use integers internally if needed

**Consequences:**
- Larger storage (16 bytes vs 4/8 bytes)
- No natural ordering (but that's fine)
- May impact index size (mitigated by modern databases)
- Slightly slower lookups (negligible)

**Status:** Accepted

---

## ADR-013: Java Records for DTOs

**Context:**
We need to define request/response objects. We need to choose between classes and records.

**Alternatives:**
1. **Traditional classes** - Mutable POJOs
2. **Java records** - Immutable data carriers
3. **Lombok** - Annotation-based code generation

**Decision:**
Choose **Java records** where appropriate.

**Reasoning:**
- Built into Java 14+ (we use Java 21)
- Immutable by default
- Concise syntax
- Automatic equals(), hashCode(), toString()
- Clear intent (data carrier)
- No external dependency needed

**Consequences:**
- Immutability enforced
- Cannot extend records (but can implement interfaces)
- Need to use classes when mutability is required
- Cleaner, more maintainable code

**Status:** Accepted

---

## ADR-014: Testcontainers for Integration Testing

**Context:**
We need to test database interactions reliably. We need a strategy for integration testing.

**Alternatives:**
1. **In-memory database (H2)** - Fast but not production-like
2. **Shared test database** - Consistent but requires maintenance
3. **Testcontainers** - Real database in containers
4. **Mock repositories** - No real database

**Decision:**
Choose **Testcontainers**.

**Reasoning:**
- Tests against real PostgreSQL
- Production-like behavior
- No shared test database needed
- Each test gets fresh database
- Docker Compose already in use
- Reliable and reproducible

**Consequences:**
- Slower than H2 (but acceptable)
- Requires Docker for tests
- More realistic test environment
- Catches database-specific issues

**Status:** Accepted

---

## ADR-015: Dashboard as Separate Application

**Context:**
NEXUS CORE needs a user interface for monitoring and management. We need to decide whether to embed it in the backend or separate it.

**Alternatives:**
1. **Server-side rendering (Thymeleaf)** - Embedded in backend
2. **Single-page app in backend** - Serve static files from backend
3. **Separate Next.js application** - Independent frontend
4. **No dashboard initially** - API-only

**Decision:**
Choose **Separate Next.js application (later phase)**.

**Reasoning:**
- Clear separation of concerns
- Independent deployment and scaling
- Modern React-based UI
- TypeScript for type safety
- Better developer experience
- Can develop backend first, dashboard later

**Consequences:**
- Two applications to deploy
- CORS configuration needed
- Separate build pipeline
- Backend API must be well-designed (already planned)
- Dashboard is Phase 7 (not initial priority)

**Status:** Accepted

---

## ADR-016: Flyway for Database Migrations

**Context:**
We need a strategy for database schema versioning and migration.

**Alternatives:**
1. **Hibernate auto-DDL** - Automatic schema generation
2. **Liquibase** - Alternative migration tool
3. **Flyway** - SQL-based migrations
4. **Manual scripts** - No tooling

**Decision:**
Choose **Flyway**.

**Reasoning:**
- SQL-based migrations (explicit and reviewable)
- Already included in Spring Boot dependencies
- Simple and reliable
- Good version control integration
- Industry standard
- Prevents drift between environments

**Consequences:**
- Manual SQL required (not auto-generated)
- Explicit schema changes
- Migration history tracked
- No automatic schema updates in production

**Status:** Accepted

---

## Future Decisions to Consider

These decisions will be made when the need arises:

### FDR-001: Row-Level Security (RLS)
**Context:** Should we add PostgreSQL RLS for additional tenant isolation?
**Status:** Not decided yet - evaluate after basic implementation

### FDR-002: Read Replicas
**Context:** Should we add read replicas for query scaling?
**Status:** Not decided yet - evaluate based on query load

### FDR-003: Redis for Caching
**Context:** Should we add Redis for caching frequently accessed data?
**Status:** Not decided yet - evaluate based on performance needs

### FDR-004: Kafka for Event Streaming
**Context:** Should we add Kafka for event streaming at scale?
**Status:** Not decided yet - evaluate based on event volume

### FDR-005: Service Extraction
**Context:** Should we extract any modules to separate services?
**Status:** Not decided yet - evaluate based on scaling needs

### FDR-006: OAuth2 / SSO
**Context:** Should we add OAuth2 or SSO for enterprise customers?
**Status:** Not decided yet - evaluate based on customer demand

---

## Decision Review Process

### When to Review a Decision
- New information emerges that challenges the decision
- The context in which the decision was made changes significantly
- The consequences of the decision are causing problems
- A better alternative becomes available

### How to Change a Decision
1. Document the new context
2. Evaluate alternatives again
3. Make a new decision with updated reasoning
4. Update the status of the old decision to "Superseded"
5. Reference the new decision number
6. Communicate the change to the team

### Decision Categories
- **Accepted** - Currently in use
- **Proposed** - Under consideration
- **Deprecated** - No longer recommended but still in use
- **Superseded** - Replaced by a newer decision
