<div align="center">

# NEXUS CORE

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)]()
[![Documentation](https://img.shields.io/badge/Docs-Latest-blue.svg)](docs/)
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen.svg)](CONTRIBUTING.md)

**A Production-Grade Background Job Processing and Automation Platform**

[Features](#-features) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [Architecture](#-architecture) • [Contributing](#-contributing) • [License](#-license)

</div>

---

## 🚀 About NEXUS CORE

NEXUS CORE is a persistent background job processing, scheduling, and automation infrastructure platform. It provides software developers and businesses with a reliable way to submit, schedule, execute, monitor, and manage background tasks without building custom job-processing infrastructure.

### The Core Idea

**Submit a job. NEXUS CORE persists it, schedules it, executes it reliably, handles failures, and reports its outcome.**

### Why NEXUS CORE?

Most applications eventually require background processing—sending emails, processing files, generating reports, synchronizing services, and more. NEXUS CORE eliminates the operational complexity, duplicated engineering effort, and reliability challenges of building this infrastructure yourself.

**Built for:**
- ✅ **Reliability** - Persistent job storage with durable execution guarantees
- ✅ **Simplicity** - Clean APIs for job submission and monitoring
- ✅ **Observability** - Clear visibility into job execution and system health
- ✅ **Scalability** - Horizontal scaling without architectural rewrites
- ✅ **Security** - Multi-tenant isolation with secure authentication
- ✅ **Flexibility** - Support for immediate, delayed, and recurring schedules
- ✅ **Resilience** - Automatic retry with configurable policies
- ✅ **Integration** - Webhook notifications for lifecycle events

---

## ✨ Features

### Job Management
- 📝 **Submit Jobs** - REST API for job submission with JSON payloads
- 🔍 **Job Monitoring** - Track job status, execution history, and attempts
- ❌ **Cancellation** - Cancel jobs before or during execution
- 🔄 **Retry** - Manually retry failed jobs
- 🎯 **Priority** - Configure job priority levels (0-10)
- ⏰ **Scheduling** - Immediate, delayed, or recurring execution
- 🔐 **Idempotency** - Prevent duplicate job submissions

### Execution Engine
- ⚡ **Persistent Workers** - Poll-based worker engine with database coordination
- 🔒 **Concurrency Control** - Safe multi-worker operation with row-level locking
- ⏱️ **Lease Management** - Automatic recovery from worker failures
- 🎭 **Handler Registry** - Allowlisted job types (no arbitrary code execution)
- 📊 **Execution Tracking** - Complete audit trail of all execution attempts

### Scheduling
- 📅 **Cron Schedules** - Full cron expression support
- 🌍 **Timezone Aware** - IANA timezone support with DST handling
- 🔄 **Recurring Jobs** - Automatic schedule generation and execution
- ⏱️ **Delayed Execution** - Schedule jobs for specific future times

### Retry & Failure Management
- 🔄 **Configurable Retries** - Fixed delay, exponential backoff, jitter
- 📊 **Retry Policies** - Per-job retry configuration
- 💀 **Dead Letter Queue** - Jobs that exhaust retries are preserved
- 📝 **Error Details** - Structured error information for debugging

### Webhooks
- 🔔 **Event Notifications** - Webhook delivery for job lifecycle events
- 🔒 **Request Signing** - HMAC-SHA256 signature verification
- 🔄 **Delivery Retries** - Automatic retry with exponential backoff
- 📊 **Delivery History** - Track all delivery attempts

### Multi-Tenancy
- 🏢 **Tenant Isolation** - Complete data separation between tenants
- 🔑 **API Key Authentication** - Secure, scoped API keys per tenant
- 📊 **Usage Tracking** - Tenant-level usage statistics
- ⚖️ **Rate Limiting** - Configurable per-tenant rate limits

### Observability
- 💚 **Health Checks** - Spring Boot Actuator endpoints
- 📈 **Metrics** - Prometheus-compatible metrics
- 📝 **Structured Logging** - JSON-formatted logs for analysis
- 📊 **Execution Statistics** - Real-time job processing statistics

---

## 🏗️ Architecture

NEXUS CORE follows a **modular monolith** architecture with clear internal boundaries, enabling future service extraction if scaling requirements justify it.

### Technology Stack

**Backend:**
- Java 21 LTS
- Spring Boot 4.1.2
- Spring Security
- Spring Data JPA
- Hibernate
- Maven
- PostgreSQL
- Flyway

**Infrastructure:**
- Docker
- Docker Compose
- Testcontainers

**Monitoring:**
- Spring Boot Actuator
- Prometheus Metrics
- Structured Logging

### Key Architectural Decisions

- 🏗️ **Modular Monolith** - Single deployable unit with clear module boundaries
- 🗄️ **PostgreSQL Only** - No Redis/Kafka initially (simpler infrastructure)
- 🔒 **At-Least-Once Execution** - With idempotency controls (honest guarantees)
- 🔑 **API Key Auth** - Simple, secure authentication (OAuth2 later if needed)
- 🎭 **Handler Allowlisting** - No arbitrary code execution (security-first)
- 🔐 **Row-Level Locking** - PostgreSQL `FOR UPDATE SKIP LOCKED` for job claiming
- 📦 **Transactional Outbox** - Reliable webhook delivery without blocking

For detailed architecture documentation, see [docs/SYSTEM_ARCHITECTURE.md](docs/SYSTEM_ARCHITECTURE.md)

---

## 📚 Documentation

- [Product Vision](docs/PRODUCT_VISION.md) - Product vision, problem statement, and philosophy
- [System Architecture](docs/SYSTEM_ARCHITECTURE.md) - Architecture, modules, and data flow
- [Database Design](docs/DATABASE_DESIGN.md) - Entity relationships and schema
- [Job Lifecycle](docs/JOB_LIFECYCLE.md) - Job states, transitions, and execution
- [API Specification](docs/API_SPECIFICATION.md) - REST API documentation
- [Security Model](docs/SECURITY_MODEL.md) - Security architecture and best practices
- [Implementation Roadmap](docs/IMPLEMENTATION_ROADMAP.md) - Development phases and milestones
- [Architecture Decisions](docs/ARCHITECTURE_DECISIONS.md) - ADRs and technical decisions

---

## 🚀 Quick Start

### Prerequisites

- Java 21 LTS ([Install](https://adoptium.net/))
- Maven 3.10+ ([Install](https://maven.apache.org/install.html)) - or use included Maven Wrapper
- Docker & Docker Compose ([Install](https://docs.docker.com/get-docker/))
- Git ([Install](https://git-scm.com/downloads))

### 1. Clone the Repository

```bash
git clone https://github.com/TECHTUNE-I-T-SOLUTIONS/nexus-core.git
cd nexus-core
```

### 2. Start PostgreSQL

```bash
cd infrastructure
docker compose up -d
```

Verify PostgreSQL is running:
```bash
docker compose ps
```

### 3. Configure Environment

Copy the example environment file:
```bash
cd ../backend
cp .env.example .env
```

Edit `.env` if needed (defaults work with Docker Compose):
```env
DATABASE_URL=jdbc:postgresql://localhost:5432/nexus_core
DATABASE_USERNAME=nexus
DATABASE_PASSWORD=nexus_password
SERVER_PORT=8080
```

### 4. Build the Application

**Using Maven Wrapper (recommended):**
```bash
cd backend
./mvnw clean verify
```

**Using system Maven:**
```bash
cd backend
mvn clean verify
```

**Windows PowerShell:**
```powershell
cd backend
.\mvnw.cmd clean verify
```

### 5. Run the Application

**Using Maven Wrapper:**
```bash
cd backend
./mvnw spring-boot:run
```

**Windows PowerShell:**
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The application will start on `http://localhost:8080`

### 6. Verify Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

## 📖 Usage

### Submit a Job

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "email.send",
    "payload": {
      "to": "user@example.com",
      "subject": "Welcome",
      "body": "Hello!"
    },
    "priority": 5
  }'
```

### Get Job Status

```bash
curl -X GET http://localhost:8080/api/v1/jobs/{jobId} \
  -H "Authorization: Bearer YOUR_API_KEY"
```

### List Jobs

```bash
curl -X GET "http://localhost:8080/api/v1/jobs?page=1&limit=50" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

For complete API documentation, see [docs/API_SPECIFICATION.md](docs/API_SPECIFICATION.md)

---

## 🧪 Testing

### Run All Tests

**Using Maven Wrapper:**
```bash
cd backend
./mvnw test
```

**Windows PowerShell:**
```powershell
cd backend
.\mvnw.cmd test
```

### Run Integration Tests

**Using Maven Wrapper:**
```bash
cd backend
./mvnw verify
```

**Windows PowerShell:**
```powershell
cd backend
.\mvnw.cmd verify
```

### Run Specific Test Class

**Using Maven Wrapper:**
```bash
cd backend
./mvnw test -Dtest=JobServiceTest
```

**Windows PowerShell:**
```powershell
cd backend
.\mvnw.cmd test -Dtest=JobServiceTest
```

Tests use Testcontainers for real PostgreSQL instances, ensuring reliability.

---

## 🛠️ Development

### Project Structure

```
NEXUS-CORE/
├── backend/              # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/    # Java source code
│   │   │   └── resources/
│   │   │       ├── application.yaml
│   │   │       └── db/migration/  # Flyway migrations
│   │   └── test/        # Test code
│   ├── pom.xml
│   └── .env.example
├── dashboard/            # Next.js dashboard (future)
├── infrastructure/       # Docker Compose configs
│   └── docker-compose.yml
├── docs/                # Documentation
└── .github/             # GitHub configurations
```

### Code Style

- Use package-by-feature structure
- Follow Java 21 best practices
- Use Java records for DTOs
- Constructor injection for dependencies
- Immutability where appropriate

### Commit Conventions

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add job cancellation endpoint
fix: handle null payload in job submission
docs: update API specification
test: add integration tests for worker
chore: upgrade Spring Boot to 4.1.2
```

---

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### How to Contribute

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Write tests
5. Commit your changes (`git commit -m 'feat: add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

---

## 🗺️ Roadmap

### Phase 0: Foundation ✅
- Repository setup
- Documentation foundation
- Infrastructure configuration

### Phase 1: Core Domain (In Progress)
- Tenant and Job domain models
- Job state machine
- Initial database schema
- Repositories and DTOs

### Phase 2: API & Authentication
- API key authentication
- Job submission and management APIs
- Tenant isolation

### Phase 3: Execution Engine ✅
- Worker engine
- Job claiming with row-level locking
- Handler registry
- Lease management and recovery
- Execution attempt tracking

### Phase 4: Scheduling & Retries (Next)
- Cron scheduling
- Retry policies
- Backoff strategies

### Phase 5: Webhooks
- Webhook registration
- Event delivery
- Transactional outbox

### Phase 6: Observability
- Metrics and monitoring
- Health checks
- Execution statistics

### Phase 7: Dashboard
- Next.js application
- Job monitoring UI
- Schedule management

### Phase 8: Production Ready
- Deployment architecture
- Security hardening
- Performance testing

For detailed roadmap, see [docs/IMPLEMENTATION_ROADMAP.md](docs/IMPLEMENTATION_ROADMAP.md)

---

## 🔒 Security

### Reporting Vulnerabilities

If you discover a security vulnerability, please report it responsibly:

- Email: security@techtune.com
- Do not create public issues for security vulnerabilities
- We will acknowledge receipt and provide updates

See [SECURITY.md](SECURITY.md) for our security policy.

### Security Features

- 🔐 API key authentication with hashed storage
- 🏢 Multi-tenant isolation at all layers
- 🎭 Handler allowlisting (no arbitrary code execution)
- 🔒 Row-level locking for concurrency control
- 📊 Audit logging for sensitive operations
- ⚖️ Rate limiting per tenant
- 🚫 SQL injection prevention
- 🔒 Secret storage best practices

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Built with [Spring Boot](https://spring.io/projects/spring-boot)
- Database powered by [PostgreSQL](https://www.postgresql.org/)
- Testing with [Testcontainers](https://www.testcontainers.org/)
- Icons by [Shields.io](https://shields.io/)

---

## 📞 Support

- 📖 [Documentation](docs/)
- 🐛 [Issue Tracker](https://github.com/TECHTUNE-I-T-SOLUTIONS/nexus-core/issues)
- 💬 [Discussions](https://github.com/TECHTUNE-I-T-SOLUTIONS/nexus-core/discussions)
- 📧 Email: support@techtune.com

---

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=TECHTUNE-I-T-SOLUTIONS/nexus-core&type=Date)](https://star-history.com/#TECHTUNE-I-T-SOLUTIONS/nexus-core&Date)

---

<div align="center">

**Developed by [TECHTUNE I.T SOLUTIONS](https://github.com/TECHTUNE-I-T-SOLUTIONS)**

[⬆ Back to Top](#nexus-core)

</div>
