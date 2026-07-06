# ADR-003: Backend-Mediated Browser Session Authentication

- Status: Accepted
- Date: 2026-07-06

## Context

The Airport Ops learning lab uses Keycloak as its authentication provider and the
IAM database as its application authorization source. The browser experience must
use the application's own login page; end users must not be redirected to or
interact with a visible Keycloak login page.

The existing API accepts Keycloak access tokens through the standard Bearer
Authorization header. That behavior is useful for tests and manual API clients and
must remain available.

## Decision

For the local learning environment, `iam-service` will accept email and password
through `POST /auth/session/login` and exchange them with Keycloak using a dedicated
confidential client and Direct Access Grant. Access and refresh tokens will be
returned to the browser only as HttpOnly, host-only, SameSite=Lax cookies.

The dedicated client is `iam-service-session`. It is separate from the privileged
`iam-service-admin` service-account client. Browser code never receives the client
secret and never calls Keycloak directly.

Cookie-authenticated unsafe requests require a CSRF cookie/header pair. Explicit
Bearer-header requests retain their existing behavior. Authorization header input
always takes precedence over an access-token cookie and an invalid header never
falls back to the cookie.

Tokens and passwords are not stored in the IAM database, returned in response
bodies, or written to application logs.

## Local-Lab Exception

Direct Access Grant is the OAuth Resource Owner Password Credentials flow. Current
OAuth security guidance states that this grant must not be used for production
systems. It increases credential exposure and does not support modern interactive
authentication such as MFA and passkeys.

It is accepted here only because this project explicitly excludes a visible
Keycloak UI while teaching the mechanics of authentication, cookies, token refresh,
CSRF, and resource-server validation.

## Production Direction

A production system should replace this flow with one of the following:

- Authorization Code flow with a branded identity experience and a BFF.
- A server-side BFF using a standards-compliant interactive identity flow and an
  opaque browser session.
- Another standards-compliant identity architecture appropriate to the deployment.

The production design must support HTTPS-only cookies, MFA/passkeys, strict token
audience validation, rate limiting, and managed client secrets.

## Consequences

- End users interact only with Airport Ops pages.
- The browser cannot read Keycloak access or refresh tokens.
- CSRF protection is mandatory because cookies are sent automatically.
- Existing Bearer-token API clients remain compatible.
- Refresh and logout require the IAM service to communicate with Keycloak.
- The application remains intentionally coupled to a deprecated grant for local
  learning and must not promote this configuration as production-ready.

## Local Realm State

Realm import creates `iam-service-session` only when the realm is first imported.
Keycloak does not overwrite an existing realm in its persisted database. Existing
local environments must either create the client manually with the settings above
or reset only the Keycloak metadata volume and re-import the realm. The main IAM
PostgreSQL volume must not be removed.
