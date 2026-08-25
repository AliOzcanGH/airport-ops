# Airport Ops

[![CI](https://github.com/AliOzcanGH/airport-ops/actions/workflows/ci.yml/badge.svg)](https://github.com/AliOzcanGH/airport-ops/actions/workflows/ci.yml)

## Overview

Airport Ops is a learning-focused, full-stack microservices lab that models a
small piece of airport ground operations — stations, gates, flights, and
turnaround tasks — behind a proper identity and authorization boundary. It is
five independently deployable Spring Boot services plus a React operations
frontend, wired together with Keycloak (authentication), an IAM-owned
PostgreSQL schema (application authorization), Kafka (cross-service events),
Redis (read-model caching), and a GitHub Actions CI pipeline, all
orchestrated locally with a single `docker compose up`.

The project was built in weekly phases (W1–W20, see `docs/adr/phases/`), each
one adding a bounded slice of the system and, deliberately, revisiting
earlier decisions once they were tested under a new constraint — e.g. the
RSA key-format bug from W8 resurfacing in Docker (W18) and again in CI
(W19), or rate limiting only getting a real security pass in W17 once IDOR
and privilege-escalation paths already existed to attack. It is not a
production airport system: several endpoints, credentials, and
infrastructure choices intentionally favor local experimentation and
incremental learning over production hardening (see
[Known Limitations](#known-limitations)).

**Tech stack:** Java 17, Spring Boot 3.5, PostgreSQL 16, Flyway, Kafka
(KRaft), Redis, Keycloak, React + TypeScript + Vite, Docker Compose, GitHub
Actions.

## Architecture

```mermaid
graph TD
    Browser["Browser (web)"] -->|session cookie| iam[iam-service]
    iam -->|Keycloak auth + JWKS| Keycloak
    iam -->|proxy: internal token| airport[airport-service]
    iam -->|proxy: internal token| flight[flight-service]
    iam -->|proxy: internal token| report[report-service]
    flight -->|Token Relay: stations/gates| airport
    airport -->|station-events| Kafka
    flight -->|flight-events| Kafka
    Kafka --> report[report-service]
    Kafka --> audit[audit-service]
    report -.->|cache| Redis
    iam --> PG[(PostgreSQL: iam/airport/flight/report/audit schemas)]
    airport --> PG
    flight --> PG
    report --> PG
    audit --> PG
```

`iam-service` is the only service the browser talks to directly; `web`'s
nginx (or Vite's dev proxy on the host) forwards everything under `/api`
there. `iam-service` then proxies authorized requests on to
`airport-service`, `flight-service`, and `report-service`, and
`flight-service` calls `airport-service` directly (Token Relay) to validate
station and gate references during flight scheduling. `audit-service` and
`report-service` never receive direct browser or proxied traffic — they only
learn about the world by consuming Kafka events.

## Service Boundaries

| Service | Responsibility | Schema |
| --- | --- | --- |
| `iam-service` | Keycloak-backed authentication, mandatory TOTP MFA, IAM permission model, tenant/member/invitation management, and the API gateway the browser talks to | `iam` |
| `airport-service` | Stations and gates (airport reference data), publishes station lifecycle events | `airport` |
| `flight-service` | Flight lifecycle state machine and turnaround tasks, publishes flight lifecycle events | `flight` |
| `report-service` | Redis-cached read models built from `airport-service`/`flight-service` Kafka events | `report` |
| `audit-service` | Durable, append-only audit trail consumed independently from `flight-events` | `audit` |

All five schemas live in one PostgreSQL database (`airport_ops_db`) —
schema-per-service, not database-per-service (see
[Known Limitations](#known-limitations)). Keycloak owns a separate internal
PostgreSQL database for identity-provider metadata only; no application code
reads it.

## Authentication & Authorization Flow

Two concerns are kept deliberately separate: **Keycloak** answers "who is
this person," and the **IAM database** (owned by `iam-service`) answers
"what are they allowed to do." Keycloak realm roles are visible as identity
information but are never used as Spring Security authorities — IAM
permission codes are converted to `GrantedAuthority` values instead. The
browser never talks to Keycloak directly (see
[ADR-003](docs/adr/ADR-003-backend-mediated-session-auth.md)); it only ever
sees `iam-service`, over an HttpOnly, CSRF-protected session cookie.

```mermaid
sequenceDiagram
    participant Browser
    participant iam as iam-service
    participant Keycloak

    Browser->>iam: POST /auth/session/login (email + password)
    iam->>Keycloak: verify credentials (Direct Access Grant)
    Keycloak-->>iam: Keycloak access/refresh tokens
    iam-->>Browser: 200 MFA_ENROLLMENT_REQUIRED or MFA_REQUIRED (no cookies yet)
    Browser->>iam: POST /auth/session/mfa/verify (6-digit TOTP code)
    iam->>iam: decrypt pending tokens, verify TOTP, resolve IAM permissions
    iam-->>Browser: 204, HttpOnly session cookies set
    Browser->>iam: GET /auth/me (cookie)
    iam-->>Browser: identity + IAM roles/permissions
```

MFA is mandatory for every user (no SMS/email MFA, no Keycloak-hosted MFA
UI); pending Keycloak tokens and TOTP secrets are encrypted at rest with
`APP_TOTP_ENCRYPTION_KEY` while a challenge is in flight. Full details,
including the local smoke-test checklist, are in
[Mandatory TOTP MFA](#mandatory-totp-mfa) below.

Downstream of login, `iam-service` proxies requests to `airport-service`,
`flight-service`, and `report-service` using a short-lived internal token it
signs itself (see [ADR-001](docs/adr/ADR-001-downstream-authorization-strategy.md)
for why authorization is evaluated centrally in `iam-service` rather than
duplicated per downstream service). `flight-service` in turn relays that
context to `airport-service` when it needs to validate a station or gate.

## Multi-Tenancy & Security Decisions

Airport Ops is multi-tenant: every organization's stations, gates, flights,
and members are isolated from every other organization's. Isolation is
enforced with an **ownership-chain check** in each service-layer method
(resolve the resource, confirm it belongs to the caller's organization,
then act) rather than at the repository/query level — this was tested
directly in the W17 security pass (IDOR, privilege escalation, mass
assignment, token tampering, invitation-token replay all closed; see
[docs/security-review-w17.md](docs/security-review-w17.md) for the full
methodology and results). The one real vulnerability that pass found — login
brute-forcing had no rate limit — was fixed in the same phase with an
in-memory, per-account/IP limiter.

The threat model that review covers: unauthenticated external attackers, and
authenticated tenant users trying to reach another tenant's data. It
explicitly does **not** cover a malicious platform admin (platform admins
are trusted with cross-tenant access by design) or a compromised
`X-Internal-Service-Secret` (the single control between `/internal/**`
endpoints — see [Known Limitations](#known-limitations)).

## Event-Driven Architecture

Two Kafka topics carry cross-service state changes; nothing consumes them
synchronously in the request path:

| Producer | Topic | Consumers |
| --- | --- | --- |
| `airport-service` | `station-events` | `report-service` |
| `flight-service` | `flight-events` | `report-service`, `audit-service` |

`report-service` builds Redis-cached read models from both topics.
`audit-service` consumes `flight-events` independently of
`report-service`'s own consumer group, so it keeps a durable audit trail
even if reporting's cache/read-model logic changes. Every Kafka-touching
integration test uses `@EmbeddedKafka` rather than a shared broker (see
`.github/workflows/ci.yml`), so no broker container is needed in CI.

## Getting Started

Full step-by-step instructions (MFA key generation, RSA signing key setup,
health-check-based startup ordering, and a single-service-on-host debug
path) are in [Quickstart (Docker Compose full stack)](#quickstart-docker-compose-full-stack)
below. Short version:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:APP_TOTP_ENCRYPTION_KEY = [Convert]::ToBase64String($bytes)

# generate iam-service/iam-token-private-key.txt — see Quickstart step 2

docker compose up -d --build
docker compose ps   # wait for everything to report "healthy"
```

Then open `http://127.0.0.1:5173` and log in with the credentials in
[Local Demo Credentials](#local-demo-credentials).

## Demo Credentials

Only a platform administrator is seeded; a tenant and its tenant admin are
created afterward through the platform's own invitation flow (platform
admin logs in → sends a tenant invitation → invitation is accepted). See
[Local Demo Credentials](#local-demo-credentials) for the full table,
including infrastructure credentials (PostgreSQL, Keycloak admin console).

## Known Limitations

- **Schema-per-service, not database-per-service** — all five schemas share
  one PostgreSQL instance/database. Acceptable for a local lab; a real
  deployment would want a stronger blast-radius boundary between services.
- **No outbox pattern** — Kafka publishing happens alongside the
  originating database write without a transactional outbox, so a crash
  between the two can (rarely) leave them inconsistent. The invitation
  email flow has the same class of gap: if SES succeeds but the
  delivery-status update fails, the email may have gone out while the
  invitation row still shows a stale status (accepted for the MVP; see the
  Local SES section below).
- **IDOR protection is service-layer, not repository-layer** — correct
  today and locked in by regression tests, but a more fragile pattern than
  scoping every query itself (e.g. `findByIdAndOrganizationId`). See
  [docs/security-review-w17.md](docs/security-review-w17.md).
- **Login rate limiting is in-memory and single-instance** — sufficient for
  this project's single-instance topology; a multi-instance deployment
  would need a shared (e.g. Redis-backed) limiter.
- **No network-level isolation for `/internal/**` endpoints** — the shared
  `X-Internal-Service-Secret` is the only control layer; there's no
  network boundary enforcing it in `docker-compose.yml`.
- **No member deactivate/remove endpoint** — a missing feature, not a
  vulnerability; if added, it needs the same kind of self-action guard that
  already protects role changes (`CannotModifyOwnRoleException`).
- **Threat model excludes a malicious platform admin or a leaked internal
  service secret** — both are treated as fully trusted/out of scope; see
  [Multi-Tenancy & Security Decisions](#multi-tenancy--security-decisions).

## Future Work

This project is complete as a learning lab covering its original roadmap
(IAM/Keycloak integration, multi-tenant authorization, event-driven
services, Docker Compose orchestration, and CI). Remaining roadmap items
that were explicitly deferred rather than forgotten:

- A transactional outbox for Kafka publishing.
- Repository-level (not just service-level) tenant scoping, as defense in
  depth.
- A Redis-backed, multi-instance login rate limiter.
- Screenshots/demo GIFs of the operations frontend (see below).

## Screenshots / Demo

Screenshots/GIFs are not yet captured (still a future documentation pass),
but a full 15-step, URL-by-URL demo script exists and was run end-to-end
against a clean Docker Compose stack:
[docs/demo-script.md](docs/demo-script.md). It also documents the one
regression that run found in the browser-facing proxy routes.

---

## Learning Goals

- Model authentication and application authorization as separate concerns.
- Use Keycloak as an OpenID Connect identity provider.
- Resolve application roles and permissions from an IAM-owned PostgreSQL schema.
- Protect Spring endpoints with IAM permissions and `@PreAuthorize`.
- Manage database evolution with Flyway.
- Explore multi-tenant data isolation and downstream authorization strategies.
- Use Kafka and Redis in real business flows (flight/station events, cached reports).
- Exercise backend workflows through a typed React operations interface.
- Run the full stack from a single `docker compose up` and gate changes with CI.

## Current Architecture

| Component | Responsibility | Local address |
| --- | --- | --- |
| Keycloak | Authentication provider and Keycloak access-token issuer | `http://127.0.0.1:8085` |
| `iam-service` | IAM data owner, permission source, and OAuth2 Resource Server | `http://127.0.0.1:8081` |
| `airport-service` | Stations, gates, and airport reference data | `http://127.0.0.1:8082` |
| `flight-service` | Flight lifecycle and task management | `http://127.0.0.1:8083` |
| `report-service` | Read models over flight/station events, Redis-cached | `http://127.0.0.1:8084` |
| `audit-service` | Durable audit trail consumed from Kafka | `http://127.0.0.1:8086` |
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

All five backend services and the frontend build and run as containers via
`docker compose up` — see [Quickstart](#quickstart-docker-compose-full-stack).
Each service also has a standalone Gradle/Dockerfile setup, so any one of them
can still be run directly on the host during development.

## Repository Structure

```text
.
|-- airport-service/     Stations, gates, and airport reference data
|-- iam-service/         IAM, Keycloak integration, and authorization logic
|-- flight-service/      Flight lifecycle and task management
|-- report-service/      Read models and Redis-cached reports
|-- audit-service/       Durable audit trail
|-- web/                 React, TypeScript, and Vite operations shell
|-- docker/              PostgreSQL initialization and Keycloak realm import
|-- docs/adr/            Architecture decision records
|-- docs/security-review-w17.md   Security review methodology and results
`-- docker-compose.yml   Full local stack: infrastructure + all services + web
```

Each backend service directory has its own multi-stage `Dockerfile`
(Gradle build stage -> `eclipse-temurin` JRE runtime stage); `web/Dockerfile`
builds the Vite app and serves it from nginx, which also reverse-proxies
`/api` to `iam-service` inside the Docker network — the same role Vite's dev
proxy plays on the host.

## Prerequisites

- Docker Desktop with Docker Compose
- Java 17 or newer (only needed for the host-run path)
- Node.js 20.19+ and npm (only needed for the host-run path)
- PowerShell for the commands below

## Mandatory TOTP MFA

MFA is required for every Airport Ops user. The only supported MFA method is a
time-based one-time password (TOTP) generated by an authenticator app. SMS MFA,
email MFA, and a Keycloak-hosted MFA UI are not supported. Keycloak remains behind
`iam-service` and is not presented directly to users.

The user-facing browser flow is:

- First login: email and password -> QR enrollment -> 6-digit authenticator code
  -> session.
- Later logins: email and password -> 6-digit authenticator code -> session.

### Backend encryption key

`APP_TOTP_ENCRYPTION_KEY` is a required backend environment variable. It is a
backend-only secret: users never see it and never enter it. The value must be a
base64-encoded 32-byte key. `iam-service` uses it to encrypt stored TOTP secrets
and temporary pending Keycloak access-token and refresh-token payloads held while
an MFA login challenge is active.

Missing or invalid key configuration causes backend startup to fail by design.
Do not commit or log this key. Keep using the same key with the same local IAM
database. If the key changes or is lost, existing encrypted TOTP credentials and
pending challenge payloads cannot be decrypted.

Generate a key in Windows PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Copy the generated base64 value into the backend terminal environment:

```powershell
$env:APP_TOTP_ENCRYPTION_KEY="PASTE_GENERATED_BASE64_KEY"
```

### Enrollment security warning

The QR code, `otpauth` URI, and manual entry key are sensitive enrollment
material. Do not share screenshots containing the QR code or manual key in real
environments. Anyone who obtains the manual key can configure another compatible
authenticator app and generate valid MFA codes for that account.

### Browser network expectations

When the frontend runs through the local Vite proxy:

- `POST /api/auth/session/login` returns `200 OK` with either
  `MFA_ENROLLMENT_REQUIRED` or `MFA_REQUIRED`. It does not set session cookies.
- `POST /api/auth/session/mfa/verify` returns `204 No Content` after a valid code
  and sets the HttpOnly session cookies.
- `GET /api/auth/me` returns `200 OK` after successful MFA verification and
  remains the canonical current-user endpoint.

Access and refresh tokens are never returned to frontend code. The frontend does
not store MFA challenge data or decode JWTs.

### Local MFA smoke checklist

- [ ] Open a clean or incognito browser window.
- [ ] Log in with an existing user.
- [ ] Confirm the QR enrollment screen appears on the first login.
- [ ] Scan the QR code with Google Authenticator, Microsoft Authenticator, Authy,
      or another compatible authenticator app.
- [ ] Enter the current 6-digit code.
- [ ] Confirm `/api/auth/me` succeeds and the browser redirects to the expected
      workspace.
- [ ] Log out.
- [ ] Log in again with the same user.
- [ ] Confirm the authenticator-code screen appears instead of QR enrollment.
- [ ] Enter the current code and confirm the workspace redirect.
- [ ] Try one incorrect code and confirm that no login session is created.

### Local-only MFA reset note

If a developer loses access to their authenticator app or changes
`APP_TOTP_ENCRYPTION_KEY`, the encrypted MFA data in their local database may no
longer be usable. They may need to reset local MFA data or rebuild their local IAM
database. This note applies only to disposable local-development data. No
production MFA reset or recovery behavior is implemented yet.

## Quickstart (Docker Compose full stack)

The entire system — PostgreSQL, Keycloak, Kafka, Redis, all five backend
services, and the frontend — starts with a single command. No local Java,
Node, or Gradle installation is required for this path.

1. Clone the repository and generate the mandatory MFA encryption key
   (Windows PowerShell):

   ```powershell
   $bytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
   $env:APP_TOTP_ENCRYPTION_KEY = [Convert]::ToBase64String($bytes)
   ```

   This environment variable must be set in the same shell that runs
   `docker compose up`. `iam-service` fails to start without it (see
   [Mandatory TOTP MFA](#mandatory-totp-mfa)).

2. Make sure `iam-service/iam-token-private-key.txt` exists — it holds the
   RSA private key `iam-service` uses to sign its internal service-to-service
   tokens, and is bind-mounted (never baked) into the `iam-service` image.
   If the file is missing, generate a disposable local one (never reuse a
   real key — see `AGENTS.md`):

   ```powershell
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out iam-service/temp-key.pem
   openssl pkcs8 -topk8 -nocrypt -in iam-service/temp-key.pem -outform DER -out iam-service/temp-key.der
   openssl base64 -A -in iam-service/temp-key.der -out iam-service/iam-token-private-key.txt
   Remove-Item iam-service/temp-key.pem, iam-service/temp-key.der
   ```

   `iam-token-private-key.txt` must be a single line of raw base64-encoded
   PKCS8 DER — no `-----BEGIN PRIVATE KEY-----` headers, no line wrapping.
   `IamTokenConfig` base64-decodes the file's trimmed content directly; a
   PEM-formatted file (the output of `pkcs8 -topk8` without `-outform DER`,
   or of `genpkey`'s default PEM output) fails to decode.

3. Build and start everything:

   ```powershell
   docker compose up -d --build
   docker compose ps
   ```

   Compose brings services up in dependency order: `postgres` and
   `keycloak` become healthy first, then `iam-service` (which needs both),
   then `airport-service` (which needs `iam-service`'s JWKS endpoint), then
   `flight-service` (which needs `airport-service` and `kafka`), and
   `report-service` / `audit-service` (which need `kafka`, and `redis` for
   reports). `web` starts last, once `iam-service` is healthy. Each entry in
   `docker compose ps` should reach `healthy`, not just `running` — a
   container can report "running" while its Spring context is still coming
   up, which is exactly the ordering problem `depends_on: condition:
   service_healthy` in `docker-compose.yml` prevents.

4. Open `http://127.0.0.1:5173` and log in with the seeded platform
   administrator from [Local Demo Credentials](#local-demo-credentials).

To stop everything (and keep data): `docker compose down`. To also wipe
PostgreSQL and Keycloak data: `docker compose down -v`.

Service ports on the host are unchanged from previous phases: `iam-service`
on `8081`, `airport-service` on `8082`, `flight-service` on `8083`,
`report-service` on `8084`, `audit-service` on `8086`, `web` on `5173`. Kafka's
host-published port moved from `9092` to an `EXTERNAL` listener still exposed
as `127.0.0.1:9092`, so any host-side Kafka tooling keeps working unchanged;
internally, containers now reach Kafka at `kafka:9092`.

### Running a single service on the host instead

For debugging one service with a debugger attached, run the infrastructure in
Docker but that one service on the host — stop its container first
(`docker compose stop iam-service`) so the ports don't collide:

```powershell
docker compose up -d postgres keycloak kafka redis
cd iam-service
$env:APP_TOTP_ENCRYPTION_KEY="PASTE_GENERATED_BASE64_KEY"
.\gradlew.bat bootRun
```

Host-run services fall back to `127.0.0.1` addresses (`application.properties`
defaults), so no extra environment variables are needed for this mode. Start
the frontend against a host-run `iam-service` the same way as before:

```powershell
cd web
npm install
npm run dev
```

Open `http://127.0.0.1:5173`. During local development, Vite proxies `/api`
requests to `iam-service` at `http://127.0.0.1:8081`, so backend CORS changes
are not required.

Frontend public configuration is documented in `web/.env.example`. Values
prefixed with `VITE_` are browser-visible configuration and must never
contain passwords, tokens, client secrets, or administrator credentials.

Use `127.0.0.1` consistently for Keycloak and secured IAM requests when
running on the host. Mixing `localhost` and `127.0.0.1` can cause an issuer
mismatch during JWT validation, because the issuer claim on a Keycloak token
is whatever host/port `iam-service` used to request it — this is also why
the Docker Compose services all address Keycloak as `keycloak:8080`
consistently rather than mixing that with a host-facing address.

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

Only the platform administrator above is seeded. There is no pre-seeded
tenant or tenant admin — log in as the platform admin, send a tenant
invitation from the operations UI, and accept it to create the first
tenant and its tenant admin.

The Keycloak client's direct access grant (password grant) is enabled only for this
local lab and the manual verification commands below. It is not the recommended
authentication flow for production applications.

Keycloak Admin Console: `http://127.0.0.1:8085/admin`

## Local SES Invitation Email Setup

Platform invitation creation can send real invitation email through AWS SESv2.
AWS credentials are not stored in this repository. Use the AWS default credential
provider chain, such as `AWS_PROFILE`, `AWS_ACCESS_KEY_ID` /
`AWS_SECRET_ACCESS_KEY`, or your local AWS configuration.

For local testing:

- Verify the SES sender identity.
- While the SES account is in sandbox mode, verify the recipient email address too.
- Configure the AWS region with `AWS_REGION` or `app.aws.region`.
- Configure the sender with `APP_MAIL_FROM`.
- Configure the frontend accept URL with
  `APP_INVITATION_ACCEPT_BASE_URL=http://127.0.0.1:5173/invitations/accept`.
- Keep `APP_INVITATION_DEV_LINK_ENABLED=true` only for local development fallback.
- Set `APP_INVITATION_DEV_LINK_ENABLED=false` outside local development so raw
  invitation links are not returned by the API.

There is no outbox, retry, or resend endpoint for invitation email. If SES
succeeds but the delivery-status database update fails, the email may have been
sent while the invitation row still shows a stale delivery status. That consistency
gap is accepted for this project (see [Known Limitations](#known-limitations)).

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

> **Docker Compose note:** the token's `iss` claim is whatever host/port the
> token was requested through. `iam-service` validates Bearer tokens against
> `KEYCLOAK_ISSUER_URI`, which inside Docker Compose is `http://keycloak:8080/...`
> (the internal service address `iam-service` itself uses) — not
> `http://127.0.0.1:8085/...`. A token requested from the host via the
> published port above will fail issuer validation when the full stack runs
> in Compose. To get a token that validates, request it from inside the
> Docker network instead:
>
> ```powershell
> docker run --rm --network airport-ops_default curlimages/curl:latest `
>   curl -s -X POST "http://keycloak:8080/realms/airport-ops/protocol/openid-connect/token" `
>     -H "Content-Type: application/x-www-form-urlencoded" `
>     -d "grant_type=password&client_id=airport-ops-local&username=platform.admin@demo.com&password=Admin123!"
> ```
>
> This only matters for direct Bearer-token API testing. The browser flow
> through `web` never talks to Keycloak directly (see
> [ADR-003](docs/adr/ADR-003-backend-mediated-session-auth.md)) and is
> unaffected.

## Endpoint Overview

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Public | Legacy custom-login learning endpoint. It verifies the IAM demo password but is not the primary authentication flow and does not issue a token. |
| `POST` | `/auth/session/login` | Public plus CSRF | Verifies email and password and returns a mandatory TOTP enrollment or verification challenge without issuing session cookies. |
| `POST` | `/auth/session/mfa/verify` | Public plus CSRF | Verifies the challenge code and creates the HttpOnly cookie session. |
| `GET` | `/auth/keycloak/me` | Bearer token | Shows identity claims from a validated Keycloak token. |
| `GET` | `/auth/me` | Bearer token or session cookie | Canonical current-user endpoint combining Keycloak identity with the matching IAM user, roles, and permissions. |
| `GET` | `/platform/authorization/probe` | Bearer token plus `platform:invitation:create` | Temporary authorization probe used to validate permission enforcement; not a business endpoint. |
| `POST` | `/platform/invitations` | Bearer token plus `platform:invitation:create` | Creates a platform invitation. |
| `POST` | `/invitations/validate` | Public | Validates an invitation token without changing state. |
| `POST` | `/invitations/accept` | Public | Accepts an invitation and provisions IAM and Keycloak state. |

The primary browser authentication flow is the backend-mediated mandatory MFA
session flow. Direct Keycloak token retrieval remains available only for local
manual API verification and Bearer-token compatibility testing.

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

CI (`.github/workflows/ci.yml`) runs all of the following on every push and
pull request against `main`: the five backend services' test suites in
parallel (against real PostgreSQL and Redis service containers), frontend
lint/test/build, and a `docker compose build` gate. To run the same checks
locally:

```powershell
docker compose up -d postgres redis

cd iam-service
.\gradlew.bat clean test
```

The IAM test suite provides a test `JwtDecoder`, so a running Keycloak container is
not required for automated tests. PostgreSQL and the Flyway migrations are required.

Run any other backend service's tests the same way (`airport-service`,
`flight-service`, `report-service`, `audit-service`):

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
- [ADR-003: Backend-Mediated Browser Session Authentication](docs/adr/ADR-003-backend-mediated-session-auth.md)
- [Security Review — W17](docs/security-review-w17.md): IDOR, privilege
  escalation, mass assignment, token tampering, invitation replay, CSRF, rate
  limiting, and internal-endpoint exposure, tested and documented.

ADR-001 selects centralized authorization evaluation: resource services relay
the caller's context to `iam-service`, which is the single source of
permission decisions, and fail closed when no decision can be obtained.
