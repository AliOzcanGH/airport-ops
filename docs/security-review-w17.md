# Security Review — W17

W8–W16 built five services (iam, airport, flight, report, audit) with a
full tenant onboarding and operations flow, each feature landing with
its own cross-tenant/permission tests (station-ownership in W9,
gate cross-tenant in W10, flight-ownership in W11, task-ownership in
W12, audit-log tenant-isolation in W14). W17 is the first systematic,
adversarial pass across the whole system, organized around the 8
categories from the roadmap.

## Test Edilen Kategoriler ve Sonuçlar

### 1. IDOR / Broken Access Control — Kapalı

Every service's by-id lookups (`GateService.getOne`,
`FlightService.findOwnedFlight`, `TurnaroundTaskService` task lookups)
fetch by raw `findById()` and then apply a manual ownership check
(station → org, flight → org) before returning the entity — no
endpoint returns another tenant's resource. Regression coverage already
existed and was reviewed: `GateControllerIntegrationTests` (station/gate
cross-org → 404), `FlightStatusTransitionIntegrationTests` and
`TurnaroundTaskControllerIntegrationTests` (flight/task cross-org →
404), `StationControllerIntegrationTests` (list-path org mismatch →
403).

Known limitation: the ownership check lives in the service layer as a
manual filter, not as a repository-level query
(`findByIdAndOrganizationId`-style, the pattern already used in
`OrganizationMemberRepository`). It is correct today but one missing
`.filter(...)` away from a regression. Scoped out of W17 per a
deliberate scope decision — test-and-document, not refactor — and
flagged here for a future hardening pass.

### 2. Privilege Escalation — Kapalı

`InviteOrganizationMemberRequest.intendedRole` and
`UpdateMemberRoleRequest.role` are both `@Pattern(regexp =
"OPS_USER|VIEWER")`-constrained; `AIRLINE_ADMIN`/`PLATFORM_ADMIN` cannot
be requested through either DTO
(`TenantMemberRoleUpdateIntegrationTests.rejectsInvalidRoleValue`,
`.rejectsPlatformAdminAsNewRole`). The W7 self-role-update guard
(`TenantMemberRoleUpdateService`, `CannotModifyOwnRoleException`) is
still in place and tested
(`TenantMemberRoleUpdateIntegrationTests.adminCannotChangeTheirOwnRole`).

Known limitation: there is no member deactivate/remove endpoint in the
codebase at all. This isn't an access-control gap (the capability
doesn't exist to be exploited) but a missing feature — out of scope for
W17, noted here so a future implementation builds in a
self-deactivation guard from day one, mirroring
`CannotModifyOwnRoleException`.

### 3. Mass Assignment — Kapalı

No create/update DTO across any service accepts `id`, `organizationId`,
`createdAt`, or an unconstrained `role`/`status` field — organization
and resource identity are always derived from the path or the
authenticated principal, never the request body
(`CreateGateRequest`, `CreateStationRequest`, `CreateFlightRequest`,
`UpdateFlightStatusRequest`, `UpdateTaskStatusRequest`,
`InviteOrganizationMemberRequest`, `UpdateMemberRoleRequest`,
`AcceptInvitationRequest`). The role-field constraints doing double
duty here are the same ones covered under category 2.

### 4. Token Tampering — Kapalı, artık gerçek testle kanıtlandı

Previously asserted only structurally (`NimbusJwtDecoder` + JWKS in
every downstream service's `JwtDecoderConfig`) but never proven with a
real signature check — every existing integration test imports a stub
`JwtDecoder` that never validates a signature at all. W17 added
`JwtSignatureVerificationIntegrationTests` to airport-service,
flight-service, report-service, and audit-service. Each spins up the
*real* `JwtDecoderConfig` bean (no stub import) against a throwaway
local JWKS server and confirms:
- a token signed by a key not published in the JWKS → 401
- a validly-signed token whose payload is altered after signing → 401
- a token signed by the actual published key → 200 (control case,
  proves the harness itself is wired correctly)

### 5. Invitation Token Replay — Kapalı

`InvitationAcceptanceTransactionService.validateInvitation()` already
rejects a second accept of an `ACCEPTED` token with 409
`INVITATION_ALREADY_USED`, and an expired token with 410
`INVITATION_EXPIRED`, backed by a pessimistic row lock
(`findByTokenHashForUpdate`) to close the race-condition window. Fully
covered by `InvitationValidationIntegrationTests` and
`InvitationAcceptanceIntegrationTests` — reviewed, no gaps found.

### 6. Session/CSRF — Kapalı, bilinçli bir tasarım

iam-service is the only service holding a browser session; it enforces
CSRF via `CookieCsrfTokenRepository` + a double-submit token, verified
by `SessionAuthIntegrationTests` (missing/invalid CSRF header → 403
`CSRF_VALIDATION_FAILED`). The session cookie is `HttpOnly`; the CSRF
cookie is deliberately not (so client JS can echo it back), also
asserted by existing tests. The other four services are stateless
JWT-bearer resource servers with CSRF explicitly disabled — a
defensible design since there's no cookie-based session to forge a
request against, and now stated explicitly here rather than left
implicit in each `SecurityConfig`.

### 7. Rate Limiting / Brute Force — **Gerçek açık bulundu, kapatıldı**

MFA challenge verification already had lockout
(`MfaLoginChallengeEntity.recordFailedAttempt`, locks after 5 attempts)
but the primary password login (`/auth/session/login`, and the legacy
`/auth/login`) had **no attempt limiting at all** — an attacker could
brute-force a password with unlimited attempts before ever reaching the
MFA stage.

**Fix**: added `LoginAttemptGuard`
(`iam-service/.../auth/LoginAttemptGuard.java`), an in-memory per-email
lockout — 5 failed attempts locks the email for 15 minutes; a
successful login clears the counter. Wired into both
`AuthService.login()` and `SessionAuthService.login()`. A new
`LoginLockedException` maps to `429 TOO_MANY_REQUESTS` /
`LOGIN_LOCKED` in `AuthExceptionHandler`. Verified by
`LoginRateLimitIntegrationTests`:
`locksLoginAfterRepeatedFailuresThenReturns429`,
`lockoutRejectsSubsequentCorrectPasswordUntilItExpires` (a locked email
stays locked even when the attacker gets the password right),
`successfulLoginDoesNotCountTowardLockout`.

Known limitation: the guard is in-memory and per-instance — correct
for this project's single-instance topology, but a multi-instance
deployment would need a shared store (Redis is already provisioned in
`docker-compose.yml` and would be the natural choice). Documented here
rather than built, since building it isn't justified by the current
deployment shape.

### 8. Internal Endpoint Exposure — Kapalı

Both `/internal/**` endpoints (`POST /internal/audit-logs` in
audit-service, `GET /internal/organizations/{orgId}/operational-summary`
in report-service) are guarded by `InternalServiceSecretFilter`
checking `X-Internal-Service-Secret`, already covered end-to-end by
`InternalAuditLogControllerIntegrationTests` and
`InternalOperationalSummaryControllerIntegrationTests` (missing header
→ 401, wrong secret → 401, correct secret → success).

Known limitation: there is no network-level restriction on these
routes — `docker-compose.yml` only defines infra (postgres, redis,
kafka, keycloak) and doesn't define the application services at all, so
there's nothing to network-isolate in this project's current topology.
The shared secret header is the sole control. A real deployment adding
network policy / a private subnet for these routes would be
defense-in-depth on top of this, not a replacement for it.

## Bulunan ve Kapatılan Açıklar

| # | Açık | Durum |
|---|------|-------|
| 7 | `/auth/session/login` ve `/auth/login` şifre denemelerinde rate limit yoktu | Kapatıldı — `LoginAttemptGuard` (in-memory, 5 deneme / 15 dk kilit), commit'te `iam-service` altında |

Diğer 7 kategoride kod tarafında açık bulunmadı; bulgular ya zaten var
olan testlerle kanıtlıydı ya da (kategori 4) bu fazda gerçek bir
imza-doğrulama testiyle ilk kez kanıtlandı.

## Bilinen Sınırlamalar (Known Limitations)

- **Member deactivate/remove endpoint'i yok** — bir güvenlik açığı
  değil, eksik bir özellik. İleride eklenirse
  `CannotModifyOwnRoleException`'a benzer bir self-deactivation guard'ı
  ile birlikte gelmeli.
- **IDOR koruması repo-seviyesinde değil servis-seviyesinde** —
  bugün doğru çalışıyor ve regresyon testleriyle kilitlendi, ama
  kırılgan bir kalıp. Gelecekte `findByIdAndOrganizationId` benzeri
  repository metodlarına geçiş defense-in-depth olarak önerilir.
- **Login rate limit in-memory ve tek instance'a özel** — bu projenin
  mevcut tek-instance topolojisi için yeterli; çoklu instance'a
  geçilirse Redis-backed bir limiter'a taşınmalı.
- **`/internal/**` endpoint'lerinde network-seviyesinde izolasyon yok**
  — uygulama seviyesindeki paylaşılan secret tek kontrol katmanı;
  `docker-compose.yml` zaten uygulama servislerini tanımlamadığı için
  bu projenin kapsamında network izolasyonu uygulanacak bir yüzey yok.

## Threat Model Özeti

Bu sistem, kimlik doğrulaması yapılmamış dış saldırganlara ve
kimliği doğrulanmış ama başka bir tenant'a (organizasyona) ait
kaynaklara erişmeye çalışan kötü niyetli bir tenant kullanıcısına karşı
test edildi. Sistem, kötü niyetli bir **platform admin**'e karşı
tasarlanmadı — platform admin zaten tüm tenant'lar üzerinde tam yetkiye
sahip kabul ediliyor (bkz. `/platform/**` endpoint'leri, tenant-scope
kontrolü olmadan tasarlanmış). Aynı şekilde, `/internal/**`
endpoint'lerine erişebilen bir servis (yani `X-Internal-Service-Secret`
sızmış bir saldırgan) de bu threat model'in dışında — bu senaryoya
karşı tek savunma katmanı paylaşılan secret'tır ve bu, "Bilinen
Sınırlamalar" bölümünde açıkça not edilmiştir.
