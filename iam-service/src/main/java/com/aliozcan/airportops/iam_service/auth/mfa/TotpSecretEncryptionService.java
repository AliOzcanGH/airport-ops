package com.aliozcan.airportops.iam_service.auth.mfa;

import com.aliozcan.airportops.iam_service.config.MfaEncryptionProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TotpSecretEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom;

    public TotpSecretEncryptionService(
            MfaEncryptionProperties properties,
            SecureRandom secureRandom) {
        this.key = new SecretKeySpec(parseKey(properties.encryptionKey()), "AES");
        this.secureRandom = secureRandom;
    }

    public EncryptedValue encrypt(String plaintext) {
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(nonce));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("MFA encryption failed", exception);
        }
    }

    public String decrypt(EncryptedValue encryptedValue) {
        try {
            byte[] nonce = Base64.getDecoder().decode(encryptedValue.nonce());
            byte[] ciphertext = Base64.getDecoder().decode(encryptedValue.ciphertext());
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new IllegalStateException("MFA decryption failed", exception);
        }
    }

    private byte[] parseKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "app.mfa.encryption-key must be configured");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey);
            if (decoded.length != KEY_BYTES) {
                throw new IllegalStateException(
                        "app.mfa.encryption-key must decode to 32 bytes");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "app.mfa.encryption-key must be base64 encoded",
                    exception);
        }
    }
}
