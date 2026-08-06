package com.aliozcan.airportops.iam_service.tenant.member;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised after a role-update transaction commits; {@link MemberRoleAuditPublisher}
 * turns this into a best-effort direct HTTP audit write (not a Kafka event —
 * this is the K7B "critical IAM action" internal-secret write path).
 */
public record MemberRoleUpdatedEvent(
        UUID organizationId,
        UUID actorUserId,
        String actorEmail,
        UUID memberId,
        String previousRole,
        String newRole,
        Instant occurredAt
) {
}
