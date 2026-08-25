package com.aliozcan.airportops.iam_service.auth.token;

import com.aliozcan.airportops.iam_service.auth.UserNotProvisionedException;
import com.aliozcan.airportops.iam_service.auth.dto.IamTokenResponse;
import com.aliozcan.airportops.iam_service.config.IamTokenProperties;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.PlatformAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.TenantAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformAuthorizationRow;
import com.aliozcan.airportops.iam_service.repository.projection.TenantAuthorizationRow;
import com.aliozcan.airportops.iam_service.tenant.AmbiguousTenantContextException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class IamTokenService {

    private static final String ISSUER = "airport-ops-iam";
    // Token Relay (W10): flight-service forwards this same token unchanged to
    // airport-service, so it must carry both services' audiences. Also carries
    // audit-service's and report-service's audiences since both validate this
    // same internally-issued token via their own JWKS-backed resource server.
    private static final List<String> AUDIENCE =
            List.of("airport-service", "flight-service", "audit-service", "report-service");

    private final UserRepository userRepository;
    private final PlatformAuthorizationRepository platformAuthorizationRepository;
    private final TenantAuthorizationRepository tenantAuthorizationRepository;
    private final RSAKey signingKey;
    private final IamTokenProperties properties;

    public IamTokenService(
            UserRepository userRepository,
            PlatformAuthorizationRepository platformAuthorizationRepository,
            TenantAuthorizationRepository tenantAuthorizationRepository,
            RSAKey signingKey,
            IamTokenProperties properties) {
        this.userRepository = userRepository;
        this.platformAuthorizationRepository = platformAuthorizationRepository;
        this.tenantAuthorizationRepository = tenantAuthorizationRepository;
        this.signingKey = signingKey;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public IamTokenResponse issueToken(Jwt keycloakJwt) {
        String email = normalizedEmail(keycloakJwt);
        UserEntity user = userRepository.findActiveByEmail(email)
                .orElseThrow(UserNotProvisionedException::new);

        SortedSet<String> platformRoles = new TreeSet<>();
        SortedSet<String> platformPermissions = new TreeSet<>();
        for (PlatformAuthorizationRow row :
                platformAuthorizationRepository.findPlatformAuthorizationByUserId(user.getId())) {
            if (row.getRoleCode() != null) {
                platformRoles.add(row.getRoleCode());
            }
            if (row.getPermissionCode() != null) {
                platformPermissions.add(row.getPermissionCode());
            }
        }

        TenantWorkspace tenantWorkspace = resolveTenantWorkspace(user.getId());

        String workspace;
        String organizationId;
        String organizationStatus;
        SortedSet<String> roles;
        SortedSet<String> permissions;
        String tokenScope;

        if (!platformRoles.isEmpty()) {
            workspace = "PLATFORM";
            organizationId = null;
            organizationStatus = null;
            roles = platformRoles;
            permissions = platformPermissions;
            tokenScope = "PLATFORM_APP";
        } else if (tenantWorkspace != null) {
            workspace = "TENANT";
            organizationId = tenantWorkspace.organizationId().toString();
            organizationStatus = tenantWorkspace.organizationStatus();
            roles = tenantWorkspace.roles();
            permissions = tenantWorkspace.permissions();
            tokenScope = "TENANT_APP";
        } else {
            throw new NoWorkspaceContextException();
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.ttlSeconds());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .serializeNullClaims(true)
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .audience(AUDIENCE)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("keycloakSub", keycloakJwt.getSubject())
                .claim("email", email)
                .claim("workspace", workspace)
                .claim("organizationId", organizationId)
                .claim("organizationStatus", organizationStatus)
                .claim("roles", List.copyOf(roles))
                .claim("permissions", List.copyOf(permissions))
                .claim("tokenScope", tokenScope)
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(signingKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        try {
            JWSSigner signer = new RSASSASigner(signingKey.toRSAPrivateKey());
            signedJWT.sign(signer);
        } catch (com.nimbusds.jose.JOSEException exception) {
            throw new IllegalStateException("Failed to sign IAM token", exception);
        }

        return new IamTokenResponse(signedJWT.serialize(), properties.ttlSeconds());
    }

    private TenantWorkspace resolveTenantWorkspace(java.util.UUID userId) {
        List<TenantAuthorizationRow> rows =
                tenantAuthorizationRepository.findTenantAuthorizationByUserId(userId);
        if (rows.isEmpty()) {
            return null;
        }
        TenantAuthorizationRow first = rows.get(0);
        SortedSet<String> roles = new TreeSet<>();
        SortedSet<String> permissions = new TreeSet<>();
        for (TenantAuthorizationRow row : rows) {
            if (!first.getOrganizationId().equals(row.getOrganizationId())) {
                throw new AmbiguousTenantContextException();
            }
            if (row.getRoleCode() != null) {
                roles.add(row.getRoleCode());
            }
            if (row.getPermissionCode() != null) {
                permissions.add(row.getPermissionCode());
            }
        }
        return new TenantWorkspace(
                first.getOrganizationId(), first.getOrganizationStatus(), roles, permissions);
    }

    private String normalizedEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotProvisionedException();
        }
        return email.trim();
    }

    private record TenantWorkspace(
            java.util.UUID organizationId,
            String organizationStatus,
            SortedSet<String> roles,
            SortedSet<String> permissions) {
    }
}
