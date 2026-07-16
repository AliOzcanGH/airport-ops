package com.aliozcan.airportops.iam_service.config;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(MfaEncryptionProperties.class)
public class MfaConfig {

    @Bean
    public SecureRandom mfaSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    public Clock mfaClock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretGenerator totpSecretGenerator() {
        return new DefaultSecretGenerator();
    }

    @Bean
    public CodeGenerator totpCodeGenerator() {
        return new DefaultCodeGenerator();
    }
}
