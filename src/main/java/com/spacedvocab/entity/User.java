package com.spacedvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

/**
 * A user account. State only changes through intention-revealing methods (no public setters).
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    public static final int DEFAULT_DAILY_NEW_LIMIT = 20;
    public static final int MAX_DAILY_NEW_LIMIT = 200;
    public static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "daily_new_limit", nullable = false)
    private int dailyNewLimit;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "onboarded_at")
    private Instant onboardedAt;

    protected User() {
        // for JPA
    }

    private User(String email, String passwordHash, String displayName, Role role, ZoneId timezone) {
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.displayName = requireDisplayName(displayName);
        this.role = Objects.requireNonNull(role, "role");
        this.status = UserStatus.ACTIVE;
        this.dailyNewLimit = DEFAULT_DAILY_NEW_LIMIT;
        this.timezone = Objects.requireNonNull(timezone, "timezone").getId();
    }

    public static User newLearner(String email, String passwordHash, String displayName, ZoneId timezone) {
        return new User(email, passwordHash, displayName, Role.LEARNER, timezone);
    }

    /** Admins are created by the bootstrap and never go through learner onboarding. */
    public static User newAdmin(String email, String passwordHash, String displayName, Instant now) {
        User admin = new User(email, passwordHash, displayName, Role.ADMIN, DEFAULT_TIMEZONE);
        admin.onboardedAt = now;
        return admin;
    }

    /** Emails are compared case-insensitively (R1), so they are always stored trimmed and lower-cased. */
    public static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email").trim().toLowerCase(Locale.ROOT);
    }

    public void rename(String displayName) {
        this.displayName = requireDisplayName(displayName);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    /** UC-L3. Saving settings for the first time also completes onboarding (D6). */
    public void updateStudySettings(int dailyNewLimit, ZoneId timezone, Instant now) {
        if (dailyNewLimit < 0 || dailyNewLimit > MAX_DAILY_NEW_LIMIT) {
            throw new IllegalArgumentException("dailyNewLimit must be between 0 and " + MAX_DAILY_NEW_LIMIT);
        }
        this.dailyNewLimit = dailyNewLimit;
        this.timezone = Objects.requireNonNull(timezone, "timezone").getId();
        if (onboardedAt == null) {
            onboardedAt = now;
        }
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    public void unlock() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == UserStatus.LOCKED;
    }

    public boolean isOnboardingCompleted() {
        return onboardedAt != null;
    }

    private static String requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        return displayName.trim();
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getDailyNewLimit() {
        return dailyNewLimit;
    }

    public ZoneId getTimezone() {
        return ZoneId.of(timezone);
    }

    public Instant getOnboardedAt() {
        return onboardedAt;
    }
}
