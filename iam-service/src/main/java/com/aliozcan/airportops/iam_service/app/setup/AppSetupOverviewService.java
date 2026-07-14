package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupOverviewResponse;
import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupStepResponse;
import com.aliozcan.airportops.iam_service.auth.UserNotProvisionedException;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.repository.TenantAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.projection.TenantAuthorizationRow;
import com.aliozcan.airportops.iam_service.security.IamAuthenticationDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppSetupOverviewService {

    private final UserRepository userRepository;
    private final TenantAuthorizationRepository tenantAuthorizationRepository;

    public AppSetupOverviewService(
            UserRepository userRepository,
            TenantAuthorizationRepository tenantAuthorizationRepository) {
        this.userRepository = userRepository;
        this.tenantAuthorizationRepository = tenantAuthorizationRepository;
    }

    @Transactional(readOnly = true)
    public AppSetupOverviewResponse overview(Authentication authentication) {
        IamAuthenticationDetails details = iamDetails(authentication);
        UserEntity user = userRepository.findActiveById(details.iamUserId())
                .orElseThrow(UserNotProvisionedException::new);
        List<TenantAuthorizationRow> rows =
                tenantAuthorizationRepository.findTenantAuthorizationByUserId(user.getId());
        if (rows.isEmpty()) {
            throw new AccessDeniedException("Tenant setup requires an active tenant membership");
        }

        TenantAuthorizationRow first = rows.get(0);
        OrganizationStatus status =
                OrganizationStatus.valueOf(first.getOrganizationStatus());
        if (status == OrganizationStatus.INACTIVE) {
            throw new AccessDeniedException("Inactive tenant setup is not available");
        }

        return new AppSetupOverviewResponse(
                first.getOrganizationId(),
                first.getOrganizationName(),
                status,
                user.getPreferredLanguage(),
                placeholderSteps());
    }

    private IamAuthenticationDetails iamDetails(Authentication authentication) {
        if (authentication == null
                || !(authentication.getDetails() instanceof IamAuthenticationDetails details)
                || !details.provisioned()
                || details.iamUserId() == null) {
            throw new UserNotProvisionedException();
        }
        return details;
    }

    private List<AppSetupStepResponse> placeholderSteps() {
        // W5A placeholder only; W5D may replace these setup steps.
        return List.of(
                new AppSetupStepResponse("PROFILE", "NOT_STARTED"),
                new AppSetupStepResponse("STATION", "LOCKED"),
                new AppSetupStepResponse("REVIEW", "LOCKED"));
    }
}
