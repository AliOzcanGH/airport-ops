# ADR-001: Downstream Authorization Strategy

## Status

Accepted

## Date

2026-06-30

## Context

The Airport Operations system uses Keycloak as its authentication provider. Keycloak
issues access tokens and each backend service validates those tokens as an OAuth2
Resource Server.

Application authorization is owned by `iam-service` and its IAM database. IAM roles
and permissions are deliberately not copied into Keycloak realm roles or token
claims. Resource services such as `airport-service` and `flight-service` must
therefore obtain an IAM authorization decision before performing protected domain
operations.

The initial strategy should preserve these boundaries while remaining simple enough
for the MVP and learning phase. It must not require custom token issuance, shared IAM
database access, or authorization logic inside Keycloak.

## Decision

For the MVP and learning phase, resource services will validate Keycloak access
tokens locally and synchronously call `iam-service` with the original bearer token
to evaluate IAM database-backed permissions.

`iam-service` remains the canonical authorization source. A resource service knows
which permission its domain operation requires, but it does not calculate the
caller's IAM roles or permissions itself.

## Conceptual Authorization Flow

1. A resource service receives a request containing a Keycloak access token.
2. The resource service validates the token as an OAuth2 Resource Server.
3. The resource service determines the permission required by the domain operation.
4. It calls the internal authorization evaluation endpoint on `iam-service` and
   relays the original bearer token.
5. `iam-service` independently validates the relayed token.
6. `iam-service` derives the caller identity from the token rather than trusting
   identity fields supplied by the calling service.
7. `iam-service` evaluates the relevant IAM user, membership, role, and permission
   records.
8. `iam-service` returns an allow or deny decision.
9. The resource service executes the domain operation only when the decision allows
   it.

## Conceptual API Contract

The following contract is a design sketch. The endpoint is not implemented by this
ADR:

```http
POST /internal/authorization/evaluate
Authorization: Bearer <original-keycloak-token>
Content-Type: application/json
```

```json
{
  "permission": "station:create",
  "organizationId": "organization-uuid",
  "resourceType": "station",
  "resourceId": null
}
```

The request must not contain caller-controlled identity fields such as email,
subject, or IAM user ID. `iam-service` resolves identity exclusively from the bearer
token.

`permission` and the applicable organization context are required for tenant-scoped
decisions. `resourceType` and `resourceId` may initially be optional and can support
future resource-level or attribute-based authorization.

A successful evaluation returns an explicit `allowed` decision. The exact internal
response schema and HTTP status contract will be finalized during implementation.

## Options Considered

### Option A: Centralized Authorization Evaluation via IAM Service

**Decision: Accepted**

This option keeps authorization decisions current and preserves `iam-service` and
the IAM database as the single source of truth. Resource services do not connect to
the IAM database and do not reproduce role or permission resolution logic. It also
avoids custom token issuance and introduces distributed-system topics such as token
relay, fail-closed behavior, latency, and service availability in a controlled way.

### Option B: IAM Token Exchange

**Decision: Deferred**

In this model, `iam-service` would exchange a Keycloak token for an IAM-issued token
containing permission claims. Resource services could then authorize requests using
local token validation.

This option is deferred because it requires custom JWT issuance, RS256 key
management, a JWKS endpoint, key rotation, token expiration policy, revocation
considerations, and a strategy for stale permissions. It can be reconsidered if
authorization latency or IAM availability makes synchronous evaluation unsuitable.

### Option C: IAM Permissions in Keycloak Token Claims

**Decision: Rejected**

This model would add IAM permissions to access tokens during Keycloak token
issuance. It is rejected because it moves application authorization concerns into
the identity provider, tightly couples Keycloak to the IAM database, and weakens the
boundary in which Keycloak provides authentication while `iam-service` owns
authorization. It would also make a future identity-provider change more disruptive.

## Consequences

### Positive

- IAM permissions are evaluated from the canonical database state.
- Resource services remain isolated from the IAM database schema.
- Keycloak remains focused on authentication.
- No custom JWT, token exchange, or signing-key infrastructure is required.
- Permission policy changes take effect without waiting for permission-bearing
  access tokens to expire.

### Negative / Trade-offs

- Protected requests add a synchronous network call to `iam-service`.
- IAM latency contributes directly to resource-service latency.
- IAM availability becomes part of the protected request path.
- Resource services need token relay, timeout, and authorization-client behavior.
- High request volume may eventually require batching, caching, or another strategy.

## Security Notes

The strategy is fail-closed. If a resource service cannot obtain a valid
authorization decision, it must not execute the protected operation.

- A denied permission results in `403 Forbidden` at the resource-service boundary.
- An unavailable authorization service may result in `503 Service Unavailable`.
- Timeouts, malformed responses, and indeterminate decisions must never be treated
  as allowed.
- The original bearer token must be relayed securely and must not be logged.
- `iam-service` must validate the relayed token independently.
- Caller-supplied email, subject, or user ID must not influence identity resolution.
- The internal authorization endpoint must not be exposed as a public
  internet-facing API.
- A future implementation may require service-to-service authentication in addition
  to the relayed user bearer token.

The precise internal status-code and error-response contract is deferred to the
endpoint implementation. The fail-closed rule is not deferred.

## Multi-Tenant Boundary

Authorization and data isolation are separate controls.

`iam-service` answers whether the authenticated user has a permission within the
specified organization. The resource service remains responsible for applying the
same `organizationId` to its domain queries and mutations.

For example, a gate lookup must use an organization-aware query such as
`findGateByIdAndOrganizationId` rather than relying on permission approval and then
loading a gate by ID alone. A positive IAM decision does not replace tenant-aware
data access.

## Caching

The initial implementation will not cache authorization decisions. One synchronous
evaluation per protected request is acceptable for the MVP and provides observable
latency and load data before optimization.

Caching may be considered only after measurement. Candidate approaches include a
short-lived local Caffeine cache, a distributed Redis cache, permission-version
tracking, or explicit invalidation. Any cache design must document its stale
permission window and revocation behavior.

## Future Revisit

This decision should be revisited when measured latency, request volume, IAM
availability requirements, or service scale make synchronous centralized evaluation
unacceptable.

Future analysis may compare:

- short-TTL authorization caching;
- IAM token exchange with locally validated permission claims;
- permission versioning and revocation mechanisms;
- resilient centralized evaluation with strict timeout and circuit-breaker rules;
- resource-level and attribute-based authorization using `resourceType` and
  `resourceId`.

Any replacement must preserve fail-closed behavior, tenant isolation, and the rule
that IAM authorization policy remains independent from the identity provider.
