package com.aliozcan.airportops.audit_service.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

@Component
public class IamJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        SortedSet<String> roles = stringSet(jwt.getClaimAsStringList("roles"));
        SortedSet<String> permissions = stringSet(jwt.getClaimAsStringList("permissions"));
        String organizationIdClaim = jwt.getClaimAsString("organizationId");
        UUID organizationId = organizationIdClaim == null ? null : UUID.fromString(organizationIdClaim);
        String workspace = jwt.getClaimAsString("workspace");

        List<GrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        JwtAuthenticationToken authenticationToken =
                new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        authenticationToken.setDetails(new IamPrincipal(organizationId, workspace, roles, permissions));
        return authenticationToken;
    }

    private SortedSet<String> stringSet(List<String> values) {
        SortedSet<String> result = new TreeSet<>();
        if (values != null) {
            result.addAll(values);
        }
        return result;
    }
}
