package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.config.InternalServiceSecretProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Writes a direct (non-Kafka) audit-log entry to audit-service after a role
 * update transaction commits — the K7B "critical IAM action" path, protected
 * by a shared internal-service secret rather than an end-user JWT. Best-effort:
 * a failure here is logged and swallowed, it never rolls back or retries the
 * role update itself (no retry mechanism in this phase, per W14 scope).
 */
@org.springframework.stereotype.Component
public class MemberRoleAuditPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemberRoleAuditPublisher.class);

    private final RestClient auditServiceRestClient;
    private final InternalServiceSecretProperties secretProperties;

    public MemberRoleAuditPublisher(
            RestClient auditServiceRestClient,
            InternalServiceSecretProperties secretProperties) {
        this.auditServiceRestClient = auditServiceRestClient;
        this.secretProperties = secretProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRoleUpdated(MemberRoleUpdatedEvent event) {
        AuditLogWriteRequest request = new AuditLogWriteRequest(
                event.organizationId(),
                event.actorUserId(),
                event.actorEmail(),
                "MEMBER_ROLE_UPDATED",
                "MEMBER",
                event.memberId(),
                event.occurredAt(),
                Map.of("previousRole", String.valueOf(event.previousRole()), "newRole", event.newRole()));
        try {
            auditServiceRestClient.post()
                    .uri("/internal/audit-logs")
                    .header("X-Internal-Service-Secret", secretProperties.internalServiceSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Failed to write MEMBER_ROLE_UPDATED audit entry for member {} in organization {}",
                    event.memberId(), event.organizationId(), exception);
        }
    }

    private record AuditLogWriteRequest(
            UUID organizationId,
            UUID actorUserId,
            String actorEmail,
            String action,
            String resourceType,
            UUID resourceId,
            Instant occurredAt,
            Map<String, Object> metadata) {
    }
}
