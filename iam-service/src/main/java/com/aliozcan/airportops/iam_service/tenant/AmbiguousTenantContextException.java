package com.aliozcan.airportops.iam_service.tenant;

/**
 * Thrown when a single user's tenant-authorization rows span more than one
 * organization. Not reachable through normal application flow today: the
 * unique index {@code uq_organization_members_active_user} (see
 * V4__create_memberships_and_role_mappings.sql) guarantees at most one
 * non-deleted membership row per user, so this guards a state the schema
 * currently forbids rather than one an endpoint can produce. It exists for
 * when that invariant changes (multi-org membership support) or is violated
 * out-of-band (a migration or backfill bug) — at which point failing loudly
 * with 409 CONFLICT is preferable to silently picking the first row.
 */
public class AmbiguousTenantContextException extends RuntimeException {
}
