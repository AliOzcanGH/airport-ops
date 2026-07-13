package com.aliozcan.airportops.iam_service.platform.tenant;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationMemberStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantDetailResponse;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantDirectoryResponse;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantMemberResponse;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantSummaryResponse;
import com.aliozcan.airportops.iam_service.repository.PlatformTenantDirectoryRepository;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformTenantMemberRow;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformTenantSummaryRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class PlatformTenantDirectoryService {

    private final PlatformTenantDirectoryRepository repository;

    public PlatformTenantDirectoryService(
            PlatformTenantDirectoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PlatformTenantDirectoryResponse listTenants() {
        return new PlatformTenantDirectoryResponse(repository.findTenantSummaries()
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public PlatformTenantDetailResponse getTenantDetail(UUID organizationId) {
        PlatformTenantSummaryResponse summary = repository
                .findTenantSummaryById(organizationId)
                .map(this::toResponse)
                .orElseThrow(PlatformTenantNotFoundException::new);

        return new PlatformTenantDetailResponse(
                summary.organizationId(),
                summary.organizationName(),
                summary.organizationStatus(),
                summary.createdAt(),
                summary.memberCount(),
                summary.primaryAdminEmail(),
                members(repository.findActiveMembersByOrganizationId(organizationId))
        );
    }

    private PlatformTenantSummaryResponse toResponse(
            PlatformTenantSummaryRow row) {
        return new PlatformTenantSummaryResponse(
                row.getOrganizationId(),
                row.getOrganizationName(),
                OrganizationStatus.valueOf(row.getOrganizationStatus()),
                row.getCreatedAt(),
                row.getMemberCount() == null ? 0 : row.getMemberCount(),
                row.getPrimaryAdminEmail()
        );
    }

    private List<PlatformTenantMemberResponse> members(
            List<PlatformTenantMemberRow> rows) {
        Map<UUID, MemberAccumulator> members = new LinkedHashMap<>();
        for (PlatformTenantMemberRow row : rows) {
            MemberAccumulator member = members.computeIfAbsent(
                    row.getMemberId(),
                    ignored -> new MemberAccumulator(row));
            String roleCode = row.getRoleCode();
            if (roleCode != null && !roleCode.trim().isEmpty()) {
                member.roles.add(roleCode.trim());
            }
        }
        return members.values()
                .stream()
                .map(MemberAccumulator::toResponse)
                .toList();
    }

    private static final class MemberAccumulator {
        private final UUID memberId;
        private final UUID userId;
        private final String email;
        private final String fullName;
        private final OrganizationMemberStatus memberStatus;
        private final Instant joinedAt;
        private final SortedSet<String> roles = new TreeSet<>();

        private MemberAccumulator(PlatformTenantMemberRow row) {
            this.memberId = row.getMemberId();
            this.userId = row.getUserId();
            this.email = row.getEmail();
            this.fullName = row.getFullName();
            this.memberStatus = OrganizationMemberStatus.valueOf(
                    row.getMemberStatus());
            this.joinedAt = row.getJoinedAt();
        }

        private PlatformTenantMemberResponse toResponse() {
            return new PlatformTenantMemberResponse(
                    memberId,
                    userId,
                    email,
                    fullName,
                    memberStatus,
                    roles,
                    joinedAt
            );
        }
    }
}
