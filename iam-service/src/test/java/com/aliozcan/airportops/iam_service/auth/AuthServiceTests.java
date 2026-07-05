package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.LoginRequest;
import com.aliozcan.airportops.iam_service.repository.PlatformAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTests {

    @Test
    void rejectsNonLocalUserBeforePasswordVerification() {
        UserRepository userRepository = mock(UserRepository.class);
        PlatformAuthorizationRepository authorizationRepository =
                mock(PlatformAuthorizationRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService service = new AuthService(
                userRepository,
                authorizationRepository,
                passwordEncoder);
        when(userRepository.findActiveLocalByEmail("keycloak@demo.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest(
                "keycloak@demo.com",
                "StrongPassword123!")))
                .isInstanceOf(InvalidLoginException.class);

        verify(userRepository).findActiveLocalByEmail("keycloak@demo.com");
        verifyNoInteractions(passwordEncoder, authorizationRepository);
    }
}
