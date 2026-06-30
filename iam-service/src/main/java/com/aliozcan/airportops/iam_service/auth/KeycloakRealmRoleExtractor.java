package com.aliozcan.airportops.iam_service.auth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
public class KeycloakRealmRoleExtractor {

    public SortedSet<String> extract(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return new TreeSet<>();
        }

        Object roles = realmAccessMap.get("roles");
        if (!(roles instanceof Collection<?> roleCollection)) {
            return new TreeSet<>();
        }

        SortedSet<String> roleSet = new TreeSet<>();
        for (Object role : roleCollection) {
            if (role instanceof String roleValue) {
                roleSet.add(roleValue);
            }
        }
        return roleSet;
    }
}
