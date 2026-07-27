package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationMemberStatus;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantMemberResponse;
import com.aliozcan.airportops.iam_service.repository.PlatformTenantDirectoryRepository;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformTenantMemberRow;
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
public class TenantMemberListService {

    private final TenantMemberAccessGuard accessGuard;
    private final PlatformTenantDirectoryRepository directoryRepository;

    public TenantMemberListService(
            TenantMemberAccessGuard accessGuard,
            PlatformTenantDirectoryRepository directoryRepository) {
        this.accessGuard = accessGuard;
        this.directoryRepository = directoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PlatformTenantMemberResponse> list(
            UUID pathOrganizationId, UUID callerUserId) {
        accessGuard.requireOrganizationAdmin(callerUserId, pathOrganizationId);
        return members(directoryRepository.findActiveMembersByOrganizationId(pathOrganizationId));
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
            this.memberStatus = OrganizationMemberStatus.valueOf(row.getMemberStatus());
            this.joinedAt = row.getJoinedAt();
        }

        private PlatformTenantMemberResponse toResponse() {
            return new PlatformTenantMemberResponse(
                    memberId, userId, email, fullName, memberStatus, roles, joinedAt);
        }
    }
}
