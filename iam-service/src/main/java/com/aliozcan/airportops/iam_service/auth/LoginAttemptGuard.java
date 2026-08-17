package com.aliozcan.airportops.iam_service.auth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-email lockout for password login. Single-instance only —
 * a multi-instance deployment would need a shared store (e.g. Redis).
 */
@Component
public class LoginAttemptGuard {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptWindow> attemptsByEmail = new ConcurrentHashMap<>();

    public LoginAttemptGuard(Clock clock) {
        this.clock = clock;
    }

    public void checkNotLocked(String email) {
        AttemptWindow window = attemptsByEmail.get(normalize(email));
        if (window != null && window.isLocked(clock.instant())) {
            throw new LoginLockedException();
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        attemptsByEmail.compute(normalize(email), (key, existing) -> {
            AttemptWindow window = (existing == null || existing.isExpired(now))
                    ? new AttemptWindow()
                    : existing;
            window.recordFailure(now);
            return window;
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class AttemptWindow {
        private int failureCount;
        private Instant lockedUntil;

        void recordFailure(Instant now) {
            failureCount++;
            if (failureCount >= MAX_ATTEMPTS) {
                lockedUntil = now.plus(LOCK_DURATION);
            }
        }

        boolean isLocked(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        boolean isExpired(Instant now) {
            return lockedUntil != null && !now.isBefore(lockedUntil);
        }
    }
}
