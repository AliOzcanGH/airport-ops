package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.LoginRequest;
import com.aliozcan.airportops.iam_service.auth.dto.LoginResponse;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.PlatformAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformAuthorizationRow;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class AuthService {

    private static final String PLATFORM_TOKEN_SCOPE = "PLATFORM";

    private final UserRepository userRepository;
    private final PlatformAuthorizationRepository platformAuthorizationRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PlatformAuthorizationRepository platformAuthorizationRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.platformAuthorizationRepository = platformAuthorizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();

        UserEntity user = userRepository.findActiveByEmail(email)
                .orElseThrow(InvalidLoginException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidLoginException();
        }

        List<PlatformAuthorizationRow> authorizationRows =
                platformAuthorizationRepository.findPlatformAuthorizationByUserId(user.getId());

        SortedSet<String> roles = new TreeSet<>();
        SortedSet<String> permissions = new TreeSet<>();

        for (PlatformAuthorizationRow row : authorizationRows) {
            roles.add(row.getRoleCode());
            if (row.getPermissionCode() != null) {
                permissions.add(row.getPermissionCode());
            }
        }

        if (roles.isEmpty()) {
            throw new InvalidLoginException();
        }

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                PLATFORM_TOKEN_SCOPE,
                roles,
                permissions
        );
    }
}
