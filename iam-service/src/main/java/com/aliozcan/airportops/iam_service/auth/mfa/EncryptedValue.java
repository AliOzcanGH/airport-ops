package com.aliozcan.airportops.iam_service.auth.mfa;

public record EncryptedValue(
        String ciphertext,
        String nonce
) {
}
