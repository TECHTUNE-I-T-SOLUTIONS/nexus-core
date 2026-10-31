# Backend integration inventory

Inspected Spring Boot controllers and DTOs in Phase 7.

## Integrated
- `GET /api/v1/jobs` — list jobs; actual controller supports optional `status` and `tenantId` filters, and returns a raw `JobResponse[]` (not the paginated envelope described in the older API specification).
- `GET /api/v1/jobs/{id}` — job detail.
- `POST /api/v1/jobs/{id}/cancel` — requires `SCOPE_jobs:cancel`.
- `POST /api/v1/jobs/{id}/retry` — requires `SCOPE_jobs:retry`.

The typed client keeps the API key server-side and is used by the restricted `/api/jobs` route handler. This route is an integration boundary for previewing backend connectivity; it is not a human authentication system and must not be exposed as a shared identity in production.

## Discovered but not yet wired
- Schedules: CRUD and pause/resume at `/api/v1/schedules`.
- Webhook endpoints: CRUD at `/api/v1/webhook-endpoints`.
- Webhook deliveries: list/detail/replay at `/api/v1/webhook-deliveries`.
- Tenants: CRUD and activation at `/api/v1/tenants`.
- Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`.

## Gaps / blockers
- Authentication is stateless bearer API-key auth. There is no browser session/OAuth flow. A dashboard must not put `NEXUS_API_KEY` in client JavaScript; production needs a secure server-side proxy/session integration.
- No controller exposes `JobStatisticsService`; overview counts and webhook metrics are not available through an authenticated dashboard API.
- The API specification documents `/api/v1/webhooks`, but the implemented controller uses `/api/v1/webhook-endpoints`.
