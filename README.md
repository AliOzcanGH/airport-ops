# Mini Airport Operations Microservices Lab

Mini Airport Operations Microservices Lab is a learning-focused full-stack project for
exploring microservices, identity and access management, Keycloak, permission-based
authorization, database migrations, and multi-tenant design.

This repository is not a production-ready airport operations product. Several
endpoints, credentials, and infrastructure choices intentionally favor local
experimentation and incremental learning over production hardening.

## Learning Goals

- Model authentication and application authorization as separate concerns.
- Use Keycloak as an OpenID Connect identity provider.
- Resolve application roles and permissions from an IAM-owned PostgreSQL schema.
- Protect Spring endpoints with IAM permissions and `@PreAuthorize`.
- Manage database evolution with Flyway.
- Explore multi-tenant data isolation and downstream authorization strategies.
- Introduce Kafka and Redis infrastructure before connecting them to business flows.
- Exercise backend workflows through a typed React operations interface.

## Current Architecture

| Component | Responsibility | Local address |
| --- | --- | --- |
| Keycloak | Authentication provider and Keycloak access-token issuer | `http://127.0.0.1:8085` |
| `iam-service` | IAM data owner, permission source, and OAuth2 Resource Server | `http://127.0.0.1:8081` |
| `airport-service` | Early service skeleton with an Actuator health endpoint | `http://127.0.0.1:8082` |
| Application PostgreSQL | `airport_ops_db` with separate application schemas | `127.0.0.1:5434` |
| Keycloak PostgreSQL | Internal Keycloak metadata database; no host port | Docker network only |
| Kafka | Local single-node KRaft infrastructure | `127.0.0.1:9092` |
| Redis | Local cache infrastructure | `127.0.0.1:6379` |
| `web` | React operations shell and local backend workflow client | `http://127.0.0.1:5173` |

Application data uses one PostgreSQL database with the `iam`, `airport`, `flight`,
`report`, and `audit` schemas. Keycloak uses its own PostgreSQL service and database
for identity-provider metadata.

Keycloak is the authentication source. `iam-service` and the IAM database are the
application authorization source. Keycloak realm roles are visible as identity
information but are not used as Spring Security authorities. IAM permission codes
are converted to `GrantedAuthority` values for authorization decisions.

Kafka and Redis are available in Docker Compose but are not yet connected to all
business workflows.

## Repository Structure

```text
.
|-- airport-service/     Airport service skeleton
|-- iam-service/         IAM, Keycloak integration, and authorization logic
|-- web/                 React, TypeScript, and Vite operations shell
|-- docker/              PostgreSQL initialization and Keycloak realm import
|-- docs/adr/            Architecture decision records
`-- docker-compose.yml   Local infrastructure
```

## Prerequisites

- Docker Desktop with Docker Compose
- Java 17 or newer
- Node.js 20.19+ and npm
- PowerShell for the commands below

## Local Setup

Start all local infrastructure:

```powershell
docker compose up -d
docker compose ps
```

PostgreSQL is exposed as `5434:5432`: Spring applications connect to
`127.0.0.1:5434`, while containers use port `5432` internally.

Start `iam-service` in another terminal:

```powershell
cd iam-service
.\gradlew.bat bootRun
```

Optionally start `airport-service` in a separate terminal:

```powershell
cd airport-service
.\gradlew.bat bootRun
```

Start the frontend in another terminal:

```powershell
cd web
npm install
npm run dev
```

Open `http://127.0.0.1:5173`. During local development, Vite proxies `/api`
requests to `iam-service` at `http://127.0.0.1:8081`, so backend CORS changes are
not required.

Frontend public configuration is documented in `web/.env.example`. Values prefixed
with `VITE_` are browser-visible configuration and must never contain passwords,
tokens, client secrets, or administrator credentials.

Use `127.0.0.1` consistently for Keycloak and secured IAM requests. Mixing
`localhost` and `127.0.0.1` can cause an issuer mismatch during JWT validation.

## Local Demo Credentials

> **Local development only:** These values are intentionally committed demo
> credentials. They are not production secrets and must not be reused in any real
> environment. Production deployments must inject credentials from environment
> variables or an appropriate secret-management system.

| Purpose | Username | Password |
| --- | --- | --- |
| Application PostgreSQL | `airport_user` | `strong-pass` |
| Keycloak metadata PostgreSQL | `keycloak` | `keycloak-pass` |
| Keycloak Admin Console | `admin` | `admin-pass` |
| Demo platform administrator | `platform.admin@demo.com` | `Admin123!` |

The Keycloak client's direct access grant (password grant) is enabled only for this
local lab and the manual verification commands below. It is not the recommended
authentication flow for production applications.

Keycloak Admin Console: `http://127.0.0.1:8085/admin`

## Get a Keycloak Access Token

```powershell
$body = @{
  grant_type = "password"
  client_id = "airport-ops-local"
  username = "platform.admin@demo.com"
  password = "Admin123!"
}

$tokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8085/realms/airport-ops/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body $body
```

The access token is available as `$tokenResponse.access_token`.

## Endpoint Overview

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Public | Legacy custom-login learning endpoint. It verifies the IAM demo password but is not the primary authentication flow and does not issue a token. |
| `GET` | `/auth/keycloak/me` | Bearer token | Shows identity claims from a validated Keycloak token. |
| `GET` | `/auth/me` | Bearer token | Combines Keycloak identity with the matching IAM user, roles, and permissions. |
| `GET` | `/platform/authorization/probe` | Bearer token plus `platform:invitation:create` | Temporary K4 authorization probe. It validates permission enforcement and is not a business endpoint. |
| `POST` | `/platform/invitations` | Bearer token plus `platform:invitation:create` | Creates a platform invitation. |
| `POST` | `/invitations/validate` | Public | Validates an invitation token without changing state. |
| `POST` | `/invitations/accept` | Public | Accepts an invitation and provisions IAM and Keycloak state. |

The primary authentication flow is the Keycloak token endpoint followed by Bearer
token authentication against protected backend endpoints.

## Manual Verification

Check IAM health without a token:

```powershell
Invoke-RestMethod "http://127.0.0.1:8081/actuator/health"
```

After obtaining `$tokenResponse`, inspect the validated Keycloak identity:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://127.0.0.1:8081/auth/keycloak/me" `
  -Headers @{ Authorization = "Bearer $($tokenResponse.access_token)" }
```

Read the combined Keycloak identity and IAM authorization view:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://127.0.0.1:8081/auth/me" `
  -Headers @{ Authorization = "Bearer $($tokenResponse.access_token)" }
```

Verify permission-based endpoint protection:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://127.0.0.1:8081/platform/authorization/probe" `
  -Headers @{ Authorization = "Bearer $($tokenResponse.access_token)" }
```

Expected response:

```json
{
  "message": "Permission granted",
  "requiredPermission": "platform:invitation:create"
}
```

Calling the probe without a token must return `401 Unauthorized`:

```powershell
curl.exe -i "http://127.0.0.1:8081/platform/authorization/probe"
```

## Testing

IAM integration tests use the real local PostgreSQL database and validate the
existing Flyway state. Start PostgreSQL first:

```powershell
docker compose up -d postgres

cd iam-service
.\gradlew.bat clean test
```

The IAM test suite provides a test `JwtDecoder`, so a running Keycloak container is
not required for automated tests. PostgreSQL and the Flyway migrations are required.

Run the airport service tests separately:

```powershell
cd airport-service
.\gradlew.bat clean test
```

Run frontend checks separately:

```powershell
cd web
npm run lint
npm run test
npm run build
```

## Architecture Decision Records

- [ADR-001: Downstream Authorization Strategy](docs/adr/ADR-001-downstream-authorization-strategy.md)
- [ADR-002: Invitation Accept Provisioning Strategy](docs/adr/ADR-002-invitation-accept-provisioning-strategy.md)

ADR-001 selects centralized authorization evaluation for the learning-phase
downstream-service model. Resource services will relay the original Keycloak bearer
token to an internal IAM authorization endpoint and fail closed when no decision can
be obtained. The conceptual internal endpoint is documented but has not been
implemented.

## Project Status and Non-Goals

This repository currently demonstrates IAM persistence, Keycloak authentication,
identity-to-IAM mapping, permission-based endpoint protection, invitation
provisioning, and a minimal React operations shell. It does not yet implement
complete airport, flight, Kafka, Redis, or downstream authorization business flows.

Current non-goals include production security hardening, custom JWT issuance, token
exchange, a custom JWKS endpoint, permission caching, and production deployment
automation.
