package com.aliozcan.airportops.iam_service.security;

import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.PlatformAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformAuthorizationRow;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
public class IamJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;
    private final PlatformAuthorizationRepository platformAuthorizationRepository;

    public IamJwtAuthenticationConverter(
            UserRepository userRepository,
            PlatformAuthorizationRepository platformAuthorizationRepository) {
        this.userRepository = userRepository;
        this.platformAuthorizationRepository = platformAuthorizationRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = normalizedEmail(jwt);
        if (email == null) {
            return authenticationToken(
                    jwt,
                    List.of(),
                    IamAuthenticationDetails.unprovisioned());
        }

        Optional<UserEntity> userResult = userRepository.findActiveByEmail(email);
        if (userResult.isEmpty()) {
            return authenticationToken(
                    jwt,
                    List.of(),
                    IamAuthenticationDetails.unprovisioned());
        }

        UserEntity user = userResult.orElseThrow();
        List<PlatformAuthorizationRow> authorizationRows =
                platformAuthorizationRepository.findPlatformAuthorizationByUserId(user.getId());

        SortedSet<String> permissionCodes = new TreeSet<>();
        for (PlatformAuthorizationRow row : authorizationRows) {
            String permissionCode = row.getPermissionCode();
            if (permissionCode != null && !permissionCode.trim().isEmpty()) {
                permissionCodes.add(permissionCode.trim());
            }
        }

        List<GrantedAuthority> authorities = permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return authenticationToken(
                jwt,
                authorities,
                IamAuthenticationDetails.provisioned(user.getId()));
    }

    private String normalizedEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim();
    }

    private JwtAuthenticationToken authenticationToken(
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities,
            IamAuthenticationDetails details) {
        JwtAuthenticationToken authenticationToken =
                new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        authenticationToken.setDetails(details);
        return authenticationToken;
    }
}
