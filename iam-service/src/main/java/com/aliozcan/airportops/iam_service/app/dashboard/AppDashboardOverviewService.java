package com.aliozcan.airportops.iam_service.app.dashboard;

import com.aliozcan.airportops.iam_service.app.dashboard.dto.AppDashboardOverviewResponse;
import com.aliozcan.airportops.iam_service.auth.UserNotProvisionedException;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.security.IamAuthenticationDetails;
import com.aliozcan.airportops.iam_service.tenant.TenantContext;
import com.aliozcan.airportops.iam_service.tenant.TenantContextResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppDashboardOverviewService {

    private final UserRepository userRepository;
    private final TenantContextResolver tenantContextResolver;

    public AppDashboardOverviewService(
            UserRepository userRepository, TenantContextResolver tenantContextResolver) {
        this.userRepository = userRepository;
        this.tenantContextResolver = tenantContextResolver;
    }

    @Transactional(readOnly = true)
    public AppDashboardOverviewResponse overview(Authentication authentication) {
        IamAuthenticationDetails details = iamDetails(authentication);
        UserEntity user = userRepository.findActiveById(details.iamUserId())
                .orElseThrow(UserNotProvisionedException::new);

        TenantContext tenantContext = tenantContextResolver.resolveActiveTenantContext(user.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Tenant dashboard requires an active tenant membership"));

        return new AppDashboardOverviewResponse(
                tenantContext.organizationId(),
                tenantContext.organizationName(),
                tenantContext.organizationStatus(),
                tenantContext.roles(),
                tenantContext.permissions());
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
}
