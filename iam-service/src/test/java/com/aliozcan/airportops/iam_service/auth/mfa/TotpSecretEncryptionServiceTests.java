package com.aliozcan.airportops.iam_service.auth.mfa;

import com.aliozcan.airportops.iam_service.config.MfaEncryptionProperties;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpSecretEncryptionServiceTests {

    private static final String TEST_KEY =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void encryptsAndDecryptsStringValues() {
        TotpSecretEncryptionService service = service(TEST_KEY);

        EncryptedValue encrypted = service.encrypt("BASE32SECRET");

        assertThat(encrypted.ciphertext()).isNotEqualTo("BASE32SECRET");
        assertThat(encrypted.nonce()).isNotBlank();
        assertThat(service.decrypt(encrypted)).isEqualTo("BASE32SECRET");
    }

    @Test
    void encryptingSamePlaintextProducesDifferentCiphertextAndNonce() {
        TotpSecretEncryptionService service = service(TEST_KEY);

        EncryptedValue first = service.encrypt("same-secret");
        EncryptedValue second = service.encrypt("same-secret");

        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
        assertThat(first.nonce()).isNotEqualTo(second.nonce());
    }

    @Test
    void rejectsMissingEncryptionKey() {
        assertThatThrownBy(() -> service(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured");
    }

    @Test
    void rejectsInvalidEncryptionKeyLength() {
        assertThatThrownBy(() -> service("c2hvcnQ="))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    private TotpSecretEncryptionService service(String key) {
        return new TotpSecretEncryptionService(
                new MfaEncryptionProperties(key),
                new SecureRandom());
    }
}
