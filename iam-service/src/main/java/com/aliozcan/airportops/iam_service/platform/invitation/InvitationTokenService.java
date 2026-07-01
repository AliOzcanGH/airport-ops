package com.aliozcan.airportops.iam_service.platform.invitation;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class InvitationTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final String HASH_ALGORITHM = "SHA-256";

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedInvitationToken generate() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
        return new GeneratedInvitationToken(rawToken, hash(rawToken));
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record GeneratedInvitationToken(
            String rawToken,
            String tokenHash
    ) {
    }
}
