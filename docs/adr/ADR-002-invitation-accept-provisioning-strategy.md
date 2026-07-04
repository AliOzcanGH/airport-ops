# ADR-002: Invitation Accept and Provisioning Strategy

## Status

Accepted

## Date

2026-07-04

## Context

The Airport Operations system uses Keycloak as its authentication provider and the
IAM database as the canonical source for business identity, tenant membership,
roles, and permissions. Keycloak realm roles are not used for application
authorization. Spring Security authorities are derived from IAM permission codes.

K6 introduced platform invitations and K7A introduced public, read-only invitation
validation. An invitation contains the canonical invited email and company name.
Only a deterministic SHA-256 hash of the high-entropy invitation token is stored in
the database.

Accepting a platform invitation must create the first administrator and business
structure for a new tenant, then make that user able to authenticate through
Keycloak. PostgreSQL and Keycloak cannot participate in one atomic transaction.
The design must therefore define which system is canonical, where the transaction
boundary ends, and how partial provisioning is represented.

## Decision

Invitation acceptance will use **IAM DB first, Keycloak after-commit**.

The IAM database is the canonical business and authorization state. Keycloak is an
external authentication and login identity system. The IAM transaction will create
the user, organization, membership, role assignment, and accepted invitation before
any Keycloak Admin API call is attempted.

The future endpoint will be public because the invited person does not yet have an
account:

```http
POST /invitations/accept
Content-Type: application/json
```

```json
{
  "token": "raw-invitation-token",
  "fullName": "Pegasus Admin",
  "password": "StrongPassword123!"
}
```

The request will not accept an email field. The canonical email is always
`invitation.admin_email`. Allowing a caller to select an email independently from
the invitation token would introduce mass-assignment and account-hijacking risk.

The raw token and password are credentials. They must not be placed in URLs,
persisted in the IAM database, or written to logs. Production transport must use
TLS.

## Platform Invitation Semantics

A platform invitation creates the first administrator for a new tenant.

- The resulting membership receives the `AIRLINE_ADMIN` organization role.
- Permissions are not assigned directly to the user. They are resolved through
  `member_roles -> roles -> role_permissions -> permissions`.
- K8 will not add `intendedRole` or `invitation_type` to platform invitations.
- Organization member invitations are a separate future flow, potentially exposed
  as `POST /organizations/{orgId}/invitations` with an intended organization role.

## IAM Transaction Boundary

K8 will perform the following work in one IAM database transaction:

1. Hash the raw token with the existing deterministic SHA-256 invitation-token
   mechanism.
2. Find the invitation by `token_hash` and acquire a row-level lock using
   `SELECT FOR UPDATE` or the JPA pessimistic-write equivalent.
3. Require the invitation to be `PENDING` and not expired.
4. Reject acceptance when an active, non-deleted IAM user already exists for
   `invitation.admin_email`.
5. Create the IAM user with:
   - `email = invitation.admin_email`;
   - `full_name = request.fullName`;
   - `status = PROVISIONING`;
   - `auth_provider = KEYCLOAK`;
   - `password_hash = NULL`;
   - `keycloak_user_id = NULL`.
6. Create the organization using the invitation company name and
   `status = ONBOARDING_INCOMPLETE`.
7. Create an active organization membership linking the user and organization.
8. Resolve the canonical `AIRLINE_ADMIN` organization role and insert the
   `member_roles` mapping. A missing or incorrectly scoped role is an invariant
   failure and rolls back the transaction.
9. Mark the invitation as accepted:
   - `status = ACCEPTED`;
   - `accepted_at = now`;
   - `organization_id = the new organization ID`.
10. Commit the IAM transaction.

Any IAM constraint or business failure before commit rolls back the complete IAM
change set. This includes duplicate active users, duplicate active organizations,
invalid invitation state, and missing canonical role data.

## Concurrency

Invitation acceptance requires a pessimistic row lock. Two requests using the same
token must not both observe `PENDING` and create duplicate tenant state.

The first request acquires the lock, completes the IAM transaction, and changes the
invitation to `ACCEPTED`. The second request waits for the lock and then observes
`ACCEPTED`, returning a conflict without creating records.

An accepted invitation is not an idempotency key for retrying Keycloak
provisioning. Reusing the token after acceptance returns conflict, including when
the earlier Keycloak synchronization failed.

## Keycloak After-Commit Synchronization

After the IAM transaction commits, K8 will call the Keycloak Admin API outside the
database transaction. The database row lock and transaction must not remain open
while waiting on the network call.

On Keycloak success:

1. Create the Keycloak user with the canonical invitation email.
2. Set the submitted password in Keycloak.
3. Receive the Keycloak subject identifier.
4. In a new, short IAM transaction, set:
   - `users.keycloak_user_id = the Keycloak subject`;
   - `users.status = ACTIVE`.
5. Return full provisioning success.

The password exists only for the duration of the accept request and the Keycloak
call. It is never stored in IAM.

## Failure and Recovery Model

If Keycloak user creation or password setup fails after the IAM commit:

1. Do not roll back the committed IAM business state.
2. In a new IAM transaction, set:
   - `users.status = KEYCLOAK_SYNC_FAILED`;
   - `users.keycloak_user_id = NULL`.
3. Keep the invitation `ACCEPTED`.
4. Keep the organization `ONBOARDING_INCOMPLETE`.
5. Return partial success to the caller.

A Keycloak duplicate-email response is a synchronization failure. K8 must not find
the existing Keycloak account by email and automatically attach its subject to the
new IAM user. Possession of an invitation token does not prove ownership of a
pre-existing Keycloak identity, and automatic linking could attach tenant
authorization to the wrong account. The IAM user is marked
`KEYCLOAK_SYNC_FAILED`; any later link requires an explicit, verified recovery
process.

Recovery is deferred to K9. It may use a platform-admin/internal retry endpoint or
a scheduled reconciliation job. Because IAM does not store the password and the
invitation cannot be accepted twice, K8 will not attempt to use the accept endpoint
as a Keycloak retry mechanism. The K9 design must explicitly define how a password
is obtained or reset during recovery.

There is also a residual dual-write failure mode in which Keycloak creates the user
but the subsequent IAM update fails. In that case the IAM user may remain
`PROVISIONING` while a Keycloak identity exists. Recovery must reconcile Keycloak by
canonical email or subject and must not blindly create a duplicate identity. If the
attempt to record `KEYCLOAK_SYNC_FAILED` itself fails, the durable IAM state may also
remain `PROVISIONING`. These states require operational visibility and later
reconciliation; K8 will not claim atomic cross-system provisioning.

## Response Contract

Full provisioning success returns:

```http
201 Created
```

```json
{
  "email": "admin@pegasus.demo",
  "organizationName": "Pegasus Airlines",
  "organizationStatus": "ONBOARDING_INCOMPLETE",
  "userStatus": "ACTIVE",
  "provisioningStatus": "READY",
  "message": "Invitation accepted. You can now sign in."
}
```

An IAM success followed by Keycloak synchronization failure returns:

```http
202 Accepted
```

```json
{
  "email": "admin@pegasus.demo",
  "organizationName": "Pegasus Airlines",
  "organizationStatus": "ONBOARDING_INCOMPLETE",
  "userStatus": "KEYCLOAK_SYNC_FAILED",
  "provisioningStatus": "LOGIN_SETUP_PENDING",
  "message": "Invitation accepted, but login setup is not ready yet. Please contact platform support."
}
```

The accept endpoint will not return a JWT or access token. After account readiness,
the user signs in through Keycloak.

## Identity Mapping

K8 will store the single Keycloak subject in nullable
`users.keycloak_user_id`. This is sufficient while the lab uses one identity
provider.

The normalized alternative
`user_identities(provider, issuer, subject, user_id)` is deferred. It should be
reconsidered when multiple identity providers, multiple identities per user,
enterprise federation, or provider/issuer-specific subject mapping becomes a real
requirement.

The current email-based JWT lookup remains an MVP compatibility mechanism. The
stable Keycloak subject should become the preferred identity mapping after K8, but
that lookup transition is a separate implementation decision.

## Authentication and Authorization Guardrails

The legacy `POST /auth/login` learning endpoint supports only users whose
`auth_provider` is `LOCAL`. It must not authenticate `KEYCLOAK` users, and it must
not attempt BCrypt verification when their `password_hash` is `NULL`. Rejected
Keycloak-managed users receive the same generic invalid-credentials response as
other failed legacy-login attempts so that provider and account state are not
disclosed.

Only active, non-deleted IAM users receive IAM permission authorities. A valid
Keycloak token does not by itself grant application permissions. Users in
`PROVISIONING`, `KEYCLOAK_SYNC_FAILED`, or `INACTIVE` state receive no IAM
authorities and cannot access permission-protected endpoints. This rule must remain
true when identity lookup moves from email to `keycloak_user_id` or a future
`user_identities` model.

## Future Migration Notes for K8

K8 requires a Flyway migration for `iam.users`:

- add nullable `keycloak_user_id VARCHAR(64)` with uniqueness for non-null values;
- add `auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL'`;
- make `password_hash` nullable;
- update `chk_users_status` to allow `PROVISIONING`, `ACTIVE`,
  `KEYCLOAK_SYNC_FAILED`, and `INACTIVE`.

The default `LOCAL` provider preserves existing records, including the local
platform-admin learning account. New invitation-accepted users explicitly use
`KEYCLOAK`.

The existing organization status constraint was verified when this ADR was written
and already allows `ONBOARDING_INCOMPLETE`, `ACTIVE`, and `INACTIVE`. Organization
status will remain focused on tenant operational onboarding. It will not include
`KEYCLOAK_SYNC_FAILED`, which is a user login-provisioning concern. K8 must still
verify the effective migration state before implementation.

The existing membership status constraint already supports `ACTIVE`, and the
invitation schema already supports `ACCEPTED`, `accepted_at`, and
`organization_id`.

K8 must update the corresponding Java entity and enum mappings together with the
database migration. It must also update the legacy login lookup to require
`auth_provider = LOCAL` and preserve the active-user filter used during authority
resolution.

## Consequences

### Positive

- IAM remains the canonical source for tenant and authorization state.
- The database business change is atomic and protected against concurrent token
  use.
- No network call is made while the IAM transaction and invitation lock are open.
- Keycloak remains focused on authentication rather than application permissions.
- Partial provisioning is represented explicitly instead of being hidden.

### Negative / Trade-offs

- IAM and Keycloak cannot be updated atomically.
- A committed invitation can produce an account that is not yet able to sign in.
- Recovery requires a separate workflow because the password is not persisted.
- Operators must be able to identify `PROVISIONING` and
  `KEYCLOAK_SYNC_FAILED` users.
- A Keycloak success followed by an IAM update failure requires reconciliation.

## Alternatives Considered

### Keycloak First, IAM Database Second

Rejected. A later IAM failure could leave an authentication identity with no
canonical IAM user, tenant, membership, or authorization state. Compensating
Keycloak deletion would itself be a fallible remote operation.

### Keycloak Call Inside the IAM Transaction

Rejected. Holding a database transaction and row lock across a remote network call
would increase lock duration, amplify Keycloak latency, and still would not provide
true atomicity.

### Distributed Transaction Across PostgreSQL and Keycloak

Rejected. Keycloak does not participate in the application's database transaction,
and introducing distributed transaction coordination is inappropriate for this
lab and architecture.

### Outbox or Event-Driven Provisioning Saga

Deferred. An outbox and asynchronous worker would improve durable retry and
recovery, but adds messaging, delivery, idempotency, and operational complexity that
is intentionally outside K8.

### `user_identities` Table in K8

Deferred. The normalized model is preferable for multiple providers or identities,
but `users.keycloak_user_id` is sufficient for the single-provider MVP.

## Out of Scope

- Implementation work in K7B.
- Keycloak retry or recovery implementation.
- Scheduled reconciliation jobs.
- Outbox/event-driven provisioning.
- Airport-service onboarding and station or gate creation.
- Email or AWS SES integration.
- Organization member invitation endpoints.
- `intendedRole` and `invitation_type`.
- Direct permission assignment to users.
- Token exchange, custom IAM JWTs, or JWKS.
- Authorization caching or Redis.
- The `user_identities` table.
