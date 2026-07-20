package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupCompletionResponse;
import com.aliozcan.airportops.iam_service.domain.model.OrganizationEntity;
import com.aliozcan.airportops.iam_service.domain.model.OrganizationSetupProfileEntity;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.repository.OrganizationRepository;
import com.aliozcan.airportops.iam_service.repository.OrganizationSetupProfileRepository;
import com.aliozcan.airportops.iam_service.repository.TenantAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.security.IamAuthenticationDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppSetupCompletionServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TenantAuthorizationRepository tenantAuthorizationRepository =
            mock(TenantAuthorizationRepository.class);
    private final OrganizationRepository organizationRepository =
            mock(OrganizationRepository.class);
    private final OrganizationSetupProfileRepository profileRepository =
            mock(OrganizationSetupProfileRepository.class);
    private final Authentication authentication = mock(Authentication.class);
    private final UserEntity user = mock(UserEntity.class);

    private AppSetupCompletionService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new AppSetupCompletionService(
                userRepository,
                tenantAuthorizationRepository,
                organizationRepository,
                profileRepository);
        userId = UUID.randomUUID();
        when(authentication.getDetails())
                .thenReturn(IamAuthenticationDetails.provisioned(userId));
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
    }

    @Test
    void onboardingOrganizationIsPreferredWhenOlderActiveOrganizationExists() {
        UUID onboardingOrganizationId = UUID.randomUUID();
        OrganizationEntity organization = mock(OrganizationEntity.class);
        OrganizationSetupProfileEntity profile = completeProfile();
        when(tenantAuthorizationRepository
                .findOnboardingAirlineAdminOrganizationIdsForUpdate(userId))
                .thenReturn(List.of(onboardingOrganizationId));
        when(organizationRepository.findById(onboardingOrganizationId))
                .thenReturn(Optional.of(organization));
        when(organization.getId()).thenReturn(onboardingOrganizationId);
        when(organization.getStatus())
                .thenReturn(OrganizationStatus.ONBOARDING_INCOMPLETE)
                .thenReturn(OrganizationStatus.ACTIVE);
        when(profileRepository.findById(onboardingOrganizationId))
                .thenReturn(Optional.of(profile));

        AppSetupCompletionResponse response = service.complete(authentication);

        assertThat(response.organizationId()).isEqualTo(onboardingOrganizationId);
        assertThat(response.organizationStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        verify(tenantAuthorizationRepository, never())
                .findActiveAirlineAdminOrganizationIdsForCompletionForUpdate(userId);
        verify(organization).activate(response.completedAt());
    }

    @Test
    void activeOrganizationReturnsAlreadyCompletedWhenNoOnboardingExists() {
        when(tenantAuthorizationRepository
                .findOnboardingAirlineAdminOrganizationIdsForUpdate(userId))
                .thenReturn(List.of());
        when(tenantAuthorizationRepository
                .findActiveAirlineAdminOrganizationIdsForCompletionForUpdate(userId))
                .thenReturn(List.of(UUID.randomUUID()));

        assertThrows(SetupAlreadyCompletedException.class,
                () -> service.complete(authentication));
        verify(organizationRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void multipleOnboardingOrganizationsAreRejectedAsAmbiguous() {
        when(tenantAuthorizationRepository
                .findOnboardingAirlineAdminOrganizationIdsForUpdate(userId))
                .thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));

        assertThrows(AccessDeniedException.class,
                () -> service.complete(authentication));
        verify(tenantAuthorizationRepository, never())
                .findActiveAirlineAdminOrganizationIdsForCompletionForUpdate(userId);
        verify(organizationRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void noOnboardingOrActiveAdminOrganizationIsForbidden() {
        when(tenantAuthorizationRepository
                .findOnboardingAirlineAdminOrganizationIdsForUpdate(userId))
                .thenReturn(List.of());
        when(tenantAuthorizationRepository
                .findActiveAirlineAdminOrganizationIdsForCompletionForUpdate(userId))
                .thenReturn(List.of());

        assertThrows(AccessDeniedException.class,
                () -> service.complete(authentication));
    }

    private OrganizationSetupProfileEntity completeProfile() {
        OrganizationSetupProfileEntity profile = mock(OrganizationSetupProfileEntity.class);
        when(profile.getDisplayName()).thenReturn("W5E Airline");
        when(profile.getCountryCode()).thenReturn("TR");
        when(profile.getTimezone()).thenReturn("Europe/Istanbul");
        when(profile.getOperationsContactEmail()).thenReturn("ops@w5e.test");
        return profile;
    }
}
