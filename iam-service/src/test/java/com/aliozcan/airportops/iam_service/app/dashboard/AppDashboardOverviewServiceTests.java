package com.aliozcan.airportops.iam_service.app.dashboard;

import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.security.IamAuthenticationDetails;
import com.aliozcan.airportops.iam_service.tenant.AmbiguousTenantContextException;
import com.aliozcan.airportops.iam_service.tenant.TenantContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppDashboardOverviewServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TenantContextResolver tenantContextResolver = mock(TenantContextResolver.class);
    private final AppDashboardOverviewService service =
            new AppDashboardOverviewService(userRepository, tenantContextResolver);

    @Test
    void propagatesAmbiguousTenantContextFromResolver() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);
        UserEntity user = mock(UserEntity.class);

        when(authentication.getDetails())
                .thenReturn(IamAuthenticationDetails.provisioned(userId));
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
        when(tenantContextResolver.resolveActiveTenantContext(userId))
                .thenThrow(new AmbiguousTenantContextException());

        assertThatThrownBy(() -> service.overview(authentication))
                .isInstanceOf(AmbiguousTenantContextException.class);
    }
}
