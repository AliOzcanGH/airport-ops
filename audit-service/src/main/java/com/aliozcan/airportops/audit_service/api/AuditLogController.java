package com.aliozcan.airportops.audit_service.api;

import com.aliozcan.airportops.audit_service.domain.AuditLogRepository;
import com.aliozcan.airportops.audit_service.error.PlatformOnlyException;
import com.aliozcan.airportops.audit_service.error.TenantMismatchException;
import com.aliozcan.airportops.audit_service.security.IamPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AuditLogController {

    private static final String PLATFORM_WORKSPACE = "PLATFORM";

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/organizations/{orgId}/audit-logs")
    public List<AuditLogResponse> tenantAuditLogs(
            @PathVariable UUID orgId, Authentication authentication) {
        IamPrincipal principal = principal(authentication);
        if (!principal.permissions().contains("audit:read")) {
            throw new AccessDeniedException("audit:read permission required");
        }
        if (!orgId.equals(principal.organizationId())) {
            throw new TenantMismatchException();
        }
        return auditLogRepository.findByOrganizationIdOrderByOccurredAtDesc(orgId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @GetMapping("/platform/audit-logs")
    public List<AuditLogResponse> platformAuditLogs(Authentication authentication) {
        IamPrincipal principal = principal(authentication);
        if (!PLATFORM_WORKSPACE.equals(principal.workspace())) {
            throw new PlatformOnlyException();
        }
        if (!principal.permissions().contains("tenant:read")) {
            throw new AccessDeniedException("tenant:read permission required");
        }
        return auditLogRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private IamPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof IamPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing IAM principal details");
    }
}
