package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserProvisioningStateService {

    private final UserRepository userRepository;

    public UserProvisioningStateService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activate(UUID userId, String keycloakUserId) {
        UserEntity user = provisioningUser(userId);
        user.activateWithKeycloakSubject(keycloakUserId, Instant.now());
        userRepository.saveAndFlush(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markKeycloakSyncFailed(UUID userId) {
        UserEntity user = provisioningUser(userId);
        user.markKeycloakSyncFailed(Instant.now());
        userRepository.saveAndFlush(user);
    }

    private UserEntity provisioningUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ProvisioningInvariantException(
                        "Provisioning user is missing"));
    }
}
