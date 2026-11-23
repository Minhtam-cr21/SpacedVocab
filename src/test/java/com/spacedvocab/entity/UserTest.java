package com.spacedvocab.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-23T03:00:00Z");

    private static User learner() {
        return User.newLearner("Minh.Anh@Example.com ", "{noop}hash", "Minh Anh", User.DEFAULT_TIMEZONE);
    }

    @Test
    void newLearnerHasDefaultsAndNormalizedEmail() {
        User user = learner();

        assertThat(user.getEmail()).isEqualTo("minh.anh@example.com");
        assertThat(user.getRole()).isEqualTo(Role.LEARNER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getDailyNewLimit()).isEqualTo(20);
        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    @Test
    void newAdminSkipsOnboarding() {
        User admin = User.newAdmin("admin@example.com", "{noop}hash", "Admin", NOW);

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isOnboardingCompleted()).isTrue();
    }

    @Test
    void firstSettingsUpdateCompletesOnboardingOnlyOnce() {
        User user = learner();

        user.updateStudySettings(40, ZoneId.of("Europe/London"), NOW);
        user.updateStudySettings(10, ZoneId.of("UTC"), NOW.plusSeconds(60));

        assertThat(user.getDailyNewLimit()).isEqualTo(10);
        assertThat(user.getTimezone()).isEqualTo(ZoneId.of("UTC"));
        assertThat(user.getOnboardedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 201})
    void dailyNewLimitMustBeWithinRange(int limit) {
        User user = learner();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> user.updateStudySettings(limit, User.DEFAULT_TIMEZONE, NOW));
    }

    @Test
    void lockAndUnlock() {
        User user = learner();

        user.lock();
        assertThat(user.isLocked()).isTrue();

        user.unlock();
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    void displayNameIsTrimmedAndMustNotBeBlank() {
        User user = learner();

        user.rename("  Anh Tuấn  ");
        assertThat(user.getDisplayName()).isEqualTo("Anh Tuấn");

        assertThatIllegalArgumentException().isThrownBy(() -> user.rename("   "));
    }
}
