package com.aliozcan.airportops.flight_service.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * W17 — Token Tampering. Unlike every other integration test in this
 * module, this class deliberately does NOT import a stub JwtDecoder: it
 * keeps the real production JwtDecoderConfig (NimbusJwtDecoder + JWKS),
 * pointed at a throwaway local JWKS server, so the actual RS256 signature
 * check is what's under test — not a mock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtSignatureVerificationIntegrationTests {

    private static final RSAKey PUBLISHED_KEY = generateKey();
    private static final RSAKey UNPUBLISHED_KEY = generateKey();
    private static final HttpServer JWKS_SERVER = startJwksServer(PUBLISHED_KEY);

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void overrideJwksUri(DynamicPropertyRegistry registry) {
        registry.add("app.iam.jwks-uri", () ->
                "http://127.0.0.1:" + JWKS_SERVER.getAddress().getPort() + "/.well-known/jwks.json");
    }

    @Test
    void rejectsTokenSignedByKeyNotPublishedInJwks() throws Exception {
        String token = sign(UNPUBLISHED_KEY, validClaims(UUID.randomUUID()));
        assertUnauthorized(token);
    }

    @Test
    void rejectsTokenWithPayloadTamperedAfterSigning() throws Exception {
        String validToken = sign(PUBLISHED_KEY, validClaims(UUID.randomUUID()));
        assertUnauthorized(tamperPayload(validToken));
    }

    @Test
    void acceptsTokenSignedByThePublishedKey() throws Exception {
        UUID organizationId = UUID.randomUUID();
        String token = sign(PUBLISHED_KEY, validClaims(organizationId));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(
                "/organizations/" + organizationId + "/flights",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        // Signature verifies; a freshly-generated org simply has nothing to list.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void assertUnauthorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(
                "/organizations/" + UUID.randomUUID() + "/flights",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private JWTClaimsSet validClaims(UUID organizationId) {
        Date now = new Date();
        return new JWTClaimsSet.Builder()
                .issuer("airport-ops-iam")
                .audience("flight-service")
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + 900_000))
                .claim("workspace", "TENANT")
                .claim("organizationId", organizationId.toString())
                .claim("organizationStatus", "ACTIVE")
                .claim("roles", List.of("AIRLINE_ADMIN"))
                .claim("permissions", List.of("flight:read", "flight:create"))
                .claim("tokenScope", "TENANT_APP")
                .build();
    }

    private String sign(RSAKey key, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(PUBLISHED_KEY.getKeyID()).build(),
                claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private String tamperPayload(String token) {
        String[] parts = token.split("\\.");
        char[] chars = parts[1].toCharArray();
        chars[0] = chars[0] == 'a' ? 'b' : 'a';
        parts[1] = new String(chars);
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("w17-test-key").generate();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer startJwksServer(RSAKey publishedKey) {
        try {
            String body = new com.nimbusds.jose.jwk.JWKSet(publishedKey.toPublicJWK()).toString();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/.well-known/jwks.json", exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
