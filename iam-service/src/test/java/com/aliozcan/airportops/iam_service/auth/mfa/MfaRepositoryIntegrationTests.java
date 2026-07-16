package com.aliozcan.airportops.iam_service.auth.mfa;

import com.aliozcan.airportops.iam_service.domain.model.MfaLoginChallengeEntity;
import com.aliozcan.airportops.iam_service.domain.model.UserTotpCredentialEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.MfaChallengeStatus;
import com.aliozcan.airportops.iam_service.repository.MfaLoginChallengeRepository;
import com.aliozcan.airportops.iam_service.repository.UserTotpCredentialRepository;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest
@Sql(statements = {
        "DELETE FROM iam.mfa_login_challenges",
        "DELETE FROM iam.user_totp_credentials"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.mfa_login_challenges",
        "DELETE FROM iam.user_totp_credentials"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MfaRepositoryIntegrationTests {

    @Autowired
    private UserTotpCredentialRepository credentialRepository;

    @Autowired
    private MfaLoginChallengeRepository challengeRepository;

    @Autowired
    private TotpSecretEncryptionService encryptionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void persistsEncryptedCredentialAndFindsEnabledCredential() {
        UUID userId = platformAdminId();
        EncryptedValue encryptedSecret = encryptionService.encrypt("PLAINTEXTSECRET");

        UserTotpCredentialEntity saved = credentialRepository.saveAndFlush(
                UserTotpCredentialEntity.enabled(
                        userId,
                        encryptedSecret.ciphertext(),
                        encryptedSecret.nonce(),
                        Instant.parse("2026-07-16T12:00:00Z")));

        assertThat(credentialRepository.findEnabledByUserId(userId)).isPresent();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT secret_ciphertext, secret_nonce, status
                        FROM iam.user_totp_credentials
                        WHERE id = ?
                        """,
                saved.getId());
        assertThat(row.get("status")).isEqualTo("ENABLED");
        assertThat(row.get("secret_ciphertext")).isNotEqualTo("PLAINTEXTSECRET");
        assertThat(row.get("secret_nonce")).isNotNull();
    }

    @Test
    @Transactional
    void persistsEncryptedChallengeAndLoadsItWithPessimisticLock() {
        UUID userId = platformAdminId();
        EncryptedValue accessToken = encryptionService.encrypt("access-token");
        EncryptedValue refreshToken = encryptionService.encrypt("refresh-token");
        Instant now = Instant.parse("2026-07-16T12:00:00Z");

        MfaLoginChallengeEntity saved = challengeRepository.saveAndFlush(
                MfaLoginChallengeEntity.pendingVerify(
                        userId,
                        accessToken.ciphertext(),
                        accessToken.nonce(),
                        refreshToken.ciphertext(),
                        refreshToken.nonce(),
                        now,
                        300,
                        1800,
                        now.plusSeconds(300),
                        now));

        MfaLoginChallengeEntity locked = challengeRepository
                .findByIdForUpdate(saved.getId())
                .orElseThrow();

        assertThat(locked.getStatus()).isEqualTo(MfaChallengeStatus.PENDING);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT access_token_ciphertext, refresh_token_ciphertext
                        FROM iam.mfa_login_challenges
                        WHERE id = ?
                        """,
                saved.getId());
        assertThat(row.get("access_token_ciphertext")).isNotEqualTo("access-token");
        assertThat(row.get("refresh_token_ciphertext")).isNotEqualTo("refresh-token");
    }

    private UUID platformAdminId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.users WHERE lower(email) = 'platform.admin@demo.com'",
                UUID.class);
    }
}
