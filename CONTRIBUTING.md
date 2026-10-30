# Contributing to NEXUS CORE

Thank you for your interest in contributing to NEXUS CORE! We welcome contributions from the community.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Conventions](#commit-conventions)
- [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Getting Started

### Prerequisites

- Java 21 LTS
- Maven 3.10+
- Docker & Docker Compose
- Git

### Setup Development Environment

1. **Fork and Clone**

```bash
# Fork the repository on GitHub
git clone https://github.com/YOUR_USERNAME/nexus-core.git
cd nexus-core
```

2. **Add Upstream Remote**

```bash
git remote add upstream https://github.com/TECHTUNE-I-T-SOLUTIONS/nexus-core.git
```

3. **Start PostgreSQL**

```bash
cd infrastructure
docker compose up -d
```

4. **Build the Project**

```bash
cd ../backend
./mvnw clean verify
```

5. **Run Tests**

```bash
./mvnw test
```

## Development Workflow

### 1. Create a Branch

Create a new branch for your work:

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/your-bug-fix
# or
git checkout -b docs/your-documentation-update
```

### 2. Make Your Changes

- Write clean, readable code
- Follow the coding standards below
- Add tests for your changes
- Update documentation if needed

### 3. Test Your Changes

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=YourTestClass

# Run integration tests
./mvnw verify
```

### 4. Commit Your Changes

Follow our [commit conventions](#commit-conventions):

```bash
git add .
git commit -m "feat: add job cancellation endpoint"
```

### 5. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

### 6. Create a Pull Request

- Go to the repository on GitHub
- Click "New Pull Request"
- Select your branch
- Fill in the PR template
- Submit for review

## Coding Standards

### Java Code Style

- **Package-by-Feature Structure** - Organize code by business capability
- **Constructor Injection** - Use constructor injection for dependencies
- **Immutability** - Use `final` fields and immutable objects where possible
- **Java Records** - Use records for DTOs and value objects
- **Meaningful Names** - Use clear, descriptive names for classes, methods, and variables
- **Small Methods** - Keep methods focused and short (< 50 lines)
- **Avoid Magic Numbers** - Use constants for magic values
- **Exception Handling** - Handle exceptions intentionally, don't swallow them

### Example

```java
// Good
public record CreateJobRequest(
    @NotBlank String jobType,
    @NotNull JsonNode payload,
    @Min(0) @Max(10) Integer priority
) {}

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final JobStateMachine stateMachine;

    public JobService(JobRepository jobRepository, JobStateMachine stateMachine) {
        this.jobRepository = jobRepository;
        this.stateMachine = stateMachine;
    }

    public Job createJob(CreateJobRequest request) {
        Job job = Job.fromRequest(request);
        return jobRepository.save(job);
    }
}

// Bad
public class JS {
    JobRepository jr;
    JobStateMachine sm;

    public Job cj(CreateJobRequest r) {
        Job j = new Job();
        j.setJobType(r.jobType);
        return jr.save(j);
    }
}
```

### Spring Boot Guidelines

- Use `@Service` for business logic
- Use `@Repository` for data access
- Use `@RestController` for API endpoints
- Use `@Configuration` for configuration classes
- Avoid `@Autowired` on fields (use constructor injection)
- Use profiles for environment-specific configuration

### Database Guidelines

- All schema changes must be done via Flyway migrations
- Never use `hibernate.ddl-auto: update` in production
- Use meaningful constraint names
- Add indexes for frequently queried columns
- Use foreign keys for relationships
- Document complex queries in comments

### API Guidelines

- Use RESTful conventions
- Return appropriate HTTP status codes
- Use consistent response format
- Validate all input
- Never expose internal errors to clients
- Include API version in URL path (`/api/v1`)

## Testing Guidelines

### Test Structure

```java
// Unit test - test business logic in isolation
@SpringBootTest
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobService jobService;

    @Test
    void shouldCreateJob() {
        // Arrange
        CreateJobRequest request = new CreateJobRequest("email.send", payload, 5);

        // Act
        Job job = jobService.createJob(request);

        // Assert
        assertThat(job.getJobType()).isEqualTo("email.send");
        verify(jobRepository).save(any(Job.class));
    }
}

// Integration test - test with real database
@Testcontainers
@SpringBootTest
class JobRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void shouldSaveAndRetrieveJob() {
        // Test with real database
    }
}
```

### Testing Principles

- **Test Behavior, Not Implementation** - Test what the code does, not how
- **Arrange-Act-Assert** - Structure tests clearly
- **Descriptive Test Names** - Use `shouldDoXWhenY` format
- **One Assertion Per Test** - Keep tests focused
- **Test Edge Cases** - Don't just test happy paths
- **Mock External Dependencies** - Use mocks for external services
- **Use Testcontainers** - Test with real PostgreSQL
- **Aim for High Coverage** - >80% coverage for critical code

### Running Tests

```bash
# All tests
./mvnw test

# Unit tests only
./mvnw test -DskipITs

# Integration tests only
./mvnw verify -DskipUTs

# Specific test class
./mvnw test -Dtest=JobServiceTest

# With coverage
./mvnw test jacoco:report
```

## Commit Conventions

We follow [Conventional Commits](https://www.conventionalcommits.org/):

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation changes
- `style` - Code style changes (formatting, etc.)
- `refactor` - Code refactoring
- `test` - Adding or updating tests
- `chore` - Maintenance tasks
- `perf` - Performance improvements
- `ci` - CI/CD changes

### Examples

```bash
feat(job): add job cancellation endpoint
fix(auth): handle null API key in authentication filter
docs(readme): update quick start instructions
style(format): apply spotless formatting
refactor(worker): extract lease management to separate class
test(job): add integration tests for job repository
chore(deps): upgrade Spring Boot to 4.1.2
perf(query): add index on jobs.status column
ci(github): add build workflow
```

### Subject Rules

- Use imperative mood ("add" not "added" or "adds")
- Don't capitalize first letter
- No period at the end
- Keep it under 50 characters

### Body Rules

- Use imperative mood
- Explain what and why, not how
- Wrap at 72 characters

### Footer Rules

- Reference issues: `Closes #123`
- Break changes: `BREAKING CHANGE: description`

## Pull Request Process

### Before Submitting

- [ ] Code follows coding standards
- [ ] Tests added/updated and passing
- [ ] Documentation updated if needed
- [ ] Commit messages follow conventions
- [ ] No merge conflicts with upstream/main
- [ ] PR description filled out

### PR Title

Use the same format as commit messages:

```
feat(job): add job cancellation endpoint
```

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issue
Closes #123

## Testing
- [ ] Unit tests added
- [ ] Integration tests added
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project style
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings generated
- [ ] Tests added/updated
- [ ] All tests passing
```

### Review Process

1. Automated checks must pass (CI/CD)
2. At least one maintainer approval required
3. Address review feedback
4. Squash commits if requested
5. Merge when approved

## Reporting Issues

### Bug Reports

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.md) and include:

- Clear description of the bug
- Steps to reproduce
- Expected behavior
- Actual behavior
- Environment details (OS, Java version, etc.)
- Logs/screenshots if applicable

### Feature Requests

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.md) and include:

- Clear description of the feature
- Use case/motivation
- Proposed implementation (if known)
- Alternatives considered

### Questions

For questions, use [GitHub Discussions](https://github.com/TECHTUNE-I-T-SOLUTIONS/nexus-core/discussions) rather than issues.

## Getting Help

- 📖 Check [Documentation](docs/)
- 🐛 Search existing [Issues](https://github.com/TECHTUNE-I-T-SOLUTIONS/nexus-core/issues)
- 💬 Start a [Discussion](https://github.com/TECHTUNE-I-T-SOLUTIONS/nexus-core/discussions)
- 📧 Email: support@techtune.com

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to NEXUS CORE! 🎉
