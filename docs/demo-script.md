# End-to-End Demo Script

A 15-step walkthrough of Airport Ops from a cold `docker compose up` to a
completed turnaround, viewed through both the platform-admin and
tenant-admin workspaces. Every step names the exact URL to open and the
exact action to take. This is the script exercised before each release to
confirm the demo path still works (see [Verification](#verification)
below).

All URLs assume the default Docker Compose ports from the
[Quickstart](../README.md#quickstart-docker-compose-full-stack): `web` on
`http://127.0.0.1:5173`.

## Prerequisites

Follow [Quickstart (Docker Compose full stack)](../README.md#quickstart-docker-compose-full-stack)
first: `APP_TOTP_ENCRYPTION_KEY` set, `iam-service/iam-token-private-key.txt`
present, then `docker compose up -d --build` and `docker compose ps` shows
every service `healthy`. No AWS/SES setup is needed — the local fallback
invitation link (`APP_INVITATION_DEV_LINK_ENABLED=true` by default) is used
in step 5 instead of a real email.

You will need an authenticator app (Google Authenticator, Microsoft
Authenticator, Authy, or similar) for the two MFA enrollment steps.

## The 15 steps

### 1. Log in as the platform administrator

Open `http://127.0.0.1:5173/login`. Enter the seeded platform admin
credentials from [Local Demo Credentials](../README.md#local-demo-credentials):

- Email: `platform.admin@demo.com`
- Password: `Admin123!`

Submit. On a clean database this is the account's first login, so the
response is `MFA_ENROLLMENT_REQUIRED`, not a session.

### 2. Enroll MFA for the platform administrator

Still on `/login`, a QR code and manual entry key appear. Scan the QR code
with your authenticator app (or enter the manual key), then enter the
current 6-digit code and submit. The app sets the session cookie and
redirects to the platform workspace.

### 3. View the platform dashboard

You land on `http://127.0.0.1:5173/platform/dashboard`. Confirm it loads
without error — this is the platform admin's home screen.

### 4. Send a tenant invitation

Go to `http://127.0.0.1:5173/platform/invitations/new`. Fill in:

- Invited admin email: any address, e.g. `admin@demo-airline.test`
- Airline / organization name: e.g. `Demo Airline`

Submit. The right-hand panel shows the created invitation, including a
**Local/dev fallback accept link** — copy it (the "Copy link" button copies
it to your clipboard).

### 5. Open the invitation accept link

Paste the copied link into a new browser tab (or the same one, in an
incognito/private window if you're still logged in as the platform admin).
It resolves to `http://127.0.0.1:5173/invitations/accept?token=...` and
shows the invitation's organization name and invited email while it
validates the token.

### 6. Accept the invitation and create the tenant admin login

On the same page, once validation succeeds, fill in the onboarding form:

- Full name: e.g. `Demo Admin`
- Preferred language: English or Turkish
- Password / confirm password: a password meeting the form's validation

Submit. The page shows "Invitation accepted" with the new user and
organization status, and a **Log in** button.

### 7. Log in as the new tenant administrator

Click **Log in** (or go to `http://127.0.0.1:5173/login`). Log in with the
email from step 4 and the password from step 6.

### 8. Enroll MFA for the tenant administrator

Same as step 2: scan the QR code shown on `/login`, enter the current
6-digit code, submit.

### 9. Complete the tenant setup wizard

A newly onboarded tenant is redirected to
`http://127.0.0.1:5173/app/setup`. Fill in the organization profile form
(display name is pre-filled; fill in country code, timezone, and
operations contact email at minimum — IATA/ICAO codes and base airport are
optional) and click **Save**. Once the required fields are saved, click
**Complete setup**. You're redirected to the tenant dashboard.

### 10. View the tenant dashboard

Confirm `http://127.0.0.1:5173/app/dashboard` loads for the tenant admin.

### 11. Create a station and add a gate

Go to `http://127.0.0.1:5173/app/stations/new`. Create a station, e.g.:

- Station name: `Istanbul Hub`
- Airport code: `IST`
- Gate count: `2`

Submit, then click **Go to station** (or navigate to
`http://127.0.0.1:5173/app/stations/<stationId>`). In the **Add gate**
form, create a gate, e.g. code `A1`, terminal `T1`. Confirm the new gate
appears in the table with status `ACTIVE`.

### 12. Create a flight

Go to `http://127.0.0.1:5173/app/flights/new`. Fill in:

- Flight number: e.g. `PC123`
- Origin / destination: e.g. `IST` / `SAW`
- Scheduled departure / arrival: any future date/time, arrival after
  departure
- Assigned gate: select the gate created in step 11 (must show `ACTIVE`)

Submit. Confirm the confirmation panel shows the new flight, then click
**View flights**.

### 13. Change the flight's status

On `http://127.0.0.1:5173/app/flights`, find the flight row. In **Change
status**, select `BOARDING` and click **Apply** — confirm the badge
updates. Repeat, selecting `DEPARTED`, and confirm that transition also
applies. (Allowed transitions: `SCHEDULED → BOARDING/DELAYED/CANCELLED`,
`BOARDING → DEPARTED`.)

### 14. Complete the flight's turnaround tasks

Click the flight number to open
`http://127.0.0.1:5173/app/flights/<flightId>`. For each turnaround task
listed, select `IN_PROGRESS` in **Change status** and click **Apply**, then
repeat selecting `DONE`. Once every task is `DONE`, confirm the "Turnaround
complete" badge appears next to the section heading.

### 15. View audit logs and reports

- Tenant audit trail: `http://127.0.0.1:5173/app/audit-logs` (tenant admin
  only) — confirm entries appear for the actions above (station/gate
  creation, flight creation, status changes, task completions).
- Platform audit trail: log back in as the platform admin
  (`/login?switchAccount=true`, then the platform admin credentials) and
  open `http://127.0.0.1:5173/platform/audit-logs` — confirm the tenant
  invitation and acceptance appear.
- Reports: as the tenant admin, open
  `http://127.0.0.1:5173/app/reports`, pick the date used for the flight in
  step 12, and confirm **Daily flight summary** and **Gate utilization**
  reflect the flight and gate created above.

> **Note:** `audit-service` and `report-service` build their view purely
> from Kafka events (see
> [Event-Driven Architecture](../README.md#event-driven-architecture)), so
> a new entry can take a few seconds to appear after the action that
> produced it. Refresh the page if a step 15 entry isn't there yet.

## Verification

This script was run end-to-end against a clean stack
(`docker compose down -v` then `docker compose up -d --build`) on
**2026-08-23**, driven through the exact HTTP endpoints the frontend calls
(same requests, same session-cookie/CSRF flow) rather than through a
browser click-through. **All 15 steps now pass cleanly on a clean-volume
run**, after two regressions found during this pass were fixed:

### Regression 1 — proxy routes broke on every request through nginx

Steps 11–15 (station/gate creation, flight creation, status changes, task
completion, audit logs, reports) completed successfully in terms of
business logic on the first verification pass, but every one of them
failed when reached through `http://127.0.0.1:5173/api/...` (the browser's
path). nginx logged, for every request to these routes:

```
upstream sent invalid chunked response while reading upstream, ...,
request: "POST /api/app/stations HTTP/1.1", upstream: "http://<ip>:8081/app/stations"
```

The request completed successfully server-side (confirmed via
`docker compose exec postgres psql`) but nginx refused to forward the
response back — a real user clicking "Create station" would see a network
error with no station visibly created. Requests sent directly to
`iam-service` on `127.0.0.1:8081` (bypassing nginx) always succeeded.

**Root cause:** `/app/stations`, `/app/stations/*/gates`, `/app/flights`,
`/app/flights/*/tasks`, `/app/audit-logs`, `/platform/audit-logs`, and
`/app/reports/*` are implemented in `iam-service` as thin proxies that
forward the request to `airport-service`/`flight-service`/`audit-service`/`report-service`
and returned the downstream response via
`RestClient...retrieve().toEntity(String.class)`, which copies the
downstream response's headers — including hop-by-hop headers such as
`Transfer-Encoding` — verbatim onto `iam-service`'s own `ResponseEntity`.
Spring MVC/Tomcat then collided that copied header with its own framing
of the same body, producing an invalid chunked response that nginx
correctly rejected.

**Fix:** each of the six proxy services now builds a fresh `ResponseEntity`
from only the downstream status code and body, letting Tomcat compute its
own framing:

- `iam-service/src/main/java/.../app/station/AppStationProxyService.java`
- `iam-service/src/main/java/.../app/gate/AppGateProxyService.java`
- `iam-service/src/main/java/.../app/flight/AppFlightProxyService.java`
- `iam-service/src/main/java/.../app/audit/AppAuditProxyService.java`
- `iam-service/src/main/java/.../app/report/AppReportProxyService.java`
- `iam-service/src/main/java/.../platform/audit/PlatformAuditProxyService.java`

### Regression 2 — reports rejected the internal service token (401)

Fixing regression 1 exposed a second, previously-masked bug: `/app/reports/daily-flights`
and `/app/reports/gate-utilization` returned a bare `401 Unauthorized`
from `iam-service` itself, before ever reaching `report-service`'s
business logic — even for a tenant admin holding `report:read`.

**Root cause:** `iam-service`'s internal service-to-service token
(`IamTokenService.AUDIENCE`) was signed with the audience list
`[airport-service, flight-service, audit-service]` — `report-service` was
never added when W15 introduced it. `report-service`'s own JWT decoder
requires `report-service` in the token's `aud` claim
(`app.iam.audience=report-service`), so every internal token it received
failed audience validation. This was invisible before regression 1 was
fixed because every reports call was already failing earlier, at the
nginx layer.

**Fix:** `report-service` added to `IamTokenService.AUDIENCE`
(`iam-service/src/main/java/.../auth/token/IamTokenService.java`).

### Test coverage

`iam-service`'s full test suite (269 tests) passes after both fixes. Note:
running the suite while the full Docker Compose stack is also up can
exhaust PostgreSQL's connection limit (each of the 5 backend services
holds its own connection pool against the same shared `postgres`
instance) and produce unrelated `too many clients already` failures —
stop the app-tier containers first (`docker compose stop iam-service
airport-service flight-service report-service audit-service`, keeping
`postgres`/`redis` up) before running tests locally.

To re-run this script yourself:

```powershell
docker compose down -v
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:APP_TOTP_ENCRYPTION_KEY = [Convert]::ToBase64String($bytes)
docker compose up -d --build
docker compose ps   # wait for every service to report "healthy"
```

Then follow steps 1–15 above in order.
