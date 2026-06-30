package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.AuthMeResponse;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.PlatformAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformAuthorizationRow;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class AuthMeService {

    private final UserRepository userRepository;
    private final PlatformAuthorizationRepository platformAuthorizationRepository;
    private final KeycloakRealmRoleExtractor roleExtractor;

    public AuthMeService(
            UserRepository userRepository,
            PlatformAuthorizationRepository platformAuthorizationRepository,
            KeycloakRealmRoleExtractor roleExtractor) {
        this.userRepository = userRepository;
        this.platformAuthorizationRepository = platformAuthorizationRepository;
        this.roleExtractor = roleExtractor;
    }

    public AuthMeResponse getCurrentUser(Jwt jwt) {
        String email = normalizedEmail(jwt);
        UserEntity user = userRepository.findActiveByEmail(email)
                .orElseThrow(UserNotProvisionedException::new);

        List<PlatformAuthorizationRow> authorizationRows =
                platformAuthorizationRepository.findPlatformAuthorizationByUserId(user.getId());

        SortedSet<String> iamRoles = new TreeSet<>();
        SortedSet<String> permissions = new TreeSet<>();
        for (PlatformAuthorizationRow row : authorizationRows) {
            if (row.getRoleCode() != null) {
                iamRoles.add(row.getRoleCode());
            }
            if (row.getPermissionCode() != null) {
                permissions.add(row.getPermissionCode());
            }
        }

        return new AuthMeResponse(
                jwt.getSubject(),
                jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                email,
                jwt.getClaimAsString("preferred_username"),
                user.getId(),
                user.getStatus(),
                roleExtractor.extract(jwt),
                iamRoles,
                permissions
        );
    }

    private String normalizedEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotProvisionedException();
        }
        return email.trim();
    }
}
