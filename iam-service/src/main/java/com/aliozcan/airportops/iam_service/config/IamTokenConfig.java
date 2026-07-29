package com.aliozcan.airportops.iam_service.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(IamTokenProperties.class)
public class IamTokenConfig {

    @Bean
    public RSAKey iamSigningKey(
            IamTokenProperties properties,
            @Value("${app.iam-token.private-key-path}") String privateKeyPath) {
        String encodedKey;
        try {
            encodedKey = Files.readString(Path.of(privateKeyPath)).trim();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read app.iam-token.private-key-path: " + privateKeyPath,
                    exception);
        }
        if (encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "app.iam-token.private-key-path file must not be empty: " + privateKeyPath);
        }
        String keyId = properties.keyId();
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException(
                    "app.iam-token.key-id must be configured");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(decoded));
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                    privateKey.getModulus(), privateKey.getPublicExponent());
            java.security.interfaces.RSAPublicKey publicKey =
                    (java.security.interfaces.RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
        } catch (IllegalArgumentException | java.security.GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "app.iam-token.private-key-path file must contain a base64-encoded "
                            + "PKCS8 RSA private key: " + privateKeyPath,
                    exception);
        }
    }
}
