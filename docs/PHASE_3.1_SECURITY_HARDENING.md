# Phase 3.1 Security Hardening

## Scope
This patch addresses security findings from the Phase 3 security review, focusing on:
1. Removing plaintext API key credentials from the security context
2. Enforcing API key scopes with method-level security
3. Adding tenant isolation enforcement at the service layer
4. Adding security tests

## Changes Made

### 1. Plaintext API Key Removal
**File**: `ApiKeyAuthentication.java`
- **Before**: Stored full plaintext API key (`private final String apiKey`)
- **After**: Stores only public key ID (`private final UUID publicKeyId`)
- **Impact**: Security context no longer retains secrets
- **Credential return**: `getCredentials()` now returns public key ID instead of secret

**File**: `ApiKeyAuthenticationFilter.java`
- Updated to pass `publicKeyId` instead of full `apiKey` to authentication object

### 2. Scope Enforcement
**File**: `SecurityConfiguration.java`
- Added `@EnableMethodSecurity` to enable method-level security
- Now supports `@PreAuthorize` annotations

**File**: `JobController.java`
- Added `@PreAuthorize` annotations to all endpoints:
  - `POST /api/v1/jobs` → `@PreAuthorize("hasAuthority('SCOPE_jobs:write')")`
  - `GET /api/v1/jobs/{id}` → `@PreAuthorize("hasAuthority('SCOPE_jobs:read')")`
  - `GET /api/v1/jobs` → `@PreAuthorize("hasAuthority('SCOPE_jobs:read')")`
  - `POST /api/v1/jobs/{id}/cancel` → `@PreAuthorize("hasAuthority('SCOPE_jobs:cancel')")`
  - `POST /api/v1/jobs/{id}/retry` → `@PreAuthorize("hasAuthority('SCOPE_jobs:retry')")`

### 3. Tenant Isolation Enforcement
**File**: `JobService.java`
- Added `getAuthenticatedTenantId()` helper method
- Added tenant isolation checks to:
  - `createJob()` - Prevents creating jobs for other tenants
  - `getJob()` - Prevents accessing jobs from other tenants
  - `listJobs()` - Overrides tenant ID with authenticated tenant when present
  - `cancelJob()` - Prevents canceling jobs from other tenants
  - `retryJob()` - Prevents retrying jobs from other tenants
- **Security principle**: Tenant identity is derived from authenticated credentials, not request input

### 4. Security Tests
**File**: `ApiKeySecurityTest.java`
- Test: `shouldStoreOnlyPublicKeyIdInAuthenticationContext`
  - Verifies secret is NOT stored in authentication context
  - Verifies public key ID is stored instead
- Test: `shouldValidateInvalidSecret`
  - Verifies invalid secrets are rejected
- Tests use Testcontainers with real PostgreSQL

## Remaining Issues (Deferred)

### Actuator Endpoints
- **Status**: `/actuator/**` still permits all requests
- **Risk**: LOW (acceptable for development environment)
- **Recommendation**: Secure in production configuration

### last_used_at Write Load
- **Status**: Still updates on every authenticated request
- **Risk**: LOW (performance concern, not security issue)
- **Recommendation**: Consider async or batched updates for high-volume scenarios

### API Key Revocation API
- **Status**: `revokeApiKey()` expects database ID, but clients receive public key ID
- **Risk**: LOW (documented limitation)
- **Recommendation**: Add revocation by public key ID in future iteration

## Test Results
- Security tests: 2 passing
- Full verification: Pending

## Authorization Policies
| Operation | Required Scope | Notes |
|-----------|----------------|-------|
| Create job | `jobs:write` | Enforced via `@PreAuthorize` |
| Read job | `jobs:read` | Enforced via `@PreAuthorize` |
| List jobs | `jobs:read` | Enforced via `@PreAuthorize` |
| Cancel job | `jobs:cancel` | Enforced via `@PreAuthorize` |
| Retry job | `jobs:retry` | Enforced via `@PreAuthorize` |

## Branch
- **Branch**: `fix/api-key-scope-enforcement`
- **Commit**: (pending)
