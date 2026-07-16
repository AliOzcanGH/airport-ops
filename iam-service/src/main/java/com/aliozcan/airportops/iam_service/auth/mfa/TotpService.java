package com.aliozcan.airportops.iam_service.auth.mfa;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;

@Service
public class TotpService {

    private static final String ISSUER = "Airport Ops";
    private static final String ALGORITHM = "SHA1";
    private static final int DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final int ALLOWED_TIME_STEP_DRIFT = 1;

    private final SecretGenerator secretGenerator;
    private final CodeGenerator codeGenerator;
    private final Clock clock;

    public TotpService(
            SecretGenerator secretGenerator,
            CodeGenerator codeGenerator,
            Clock clock) {
        this.secretGenerator = secretGenerator;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String otpauthUri(String accountName, String secret) {
        String label = ISSUER + ":" + accountName;
        return UriComponentsBuilder
                .fromUriString("otpauth://totp/{label}")
                .queryParam("secret", secret)
                .queryParam("issuer", ISSUER)
                .queryParam("algorithm", ALGORITHM)
                .queryParam("digits", DIGITS)
                .queryParam("period", PERIOD_SECONDS)
                .buildAndExpand(label)
                .encode()
                .toUriString();
    }

    public boolean verify(String secret, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentBucket = Math.floorDiv(
                clock.instant().getEpochSecond(),
                PERIOD_SECONDS);
        for (int drift = -ALLOWED_TIME_STEP_DRIFT;
                drift <= ALLOWED_TIME_STEP_DRIFT;
                drift++) {
            if (matches(secret, code, currentBucket + drift)) {
                return true;
            }
        }
        return false;
    }

    String generateCodeForCurrentTime(String secret) {
        return generateCode(secret, Math.floorDiv(
                clock.instant().getEpochSecond(),
                PERIOD_SECONDS));
    }

    private boolean matches(String secret, String code, long bucket) {
        return code.equals(generateCode(secret, bucket));
    }

    private String generateCode(String secret, long bucket) {
        try {
            return codeGenerator.generate(secret, bucket);
        } catch (CodeGenerationException exception) {
            throw new IllegalStateException("TOTP code generation failed", exception);
        }
    }
}
