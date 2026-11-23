package com.spacedvocab.dto;

import com.spacedvocab.entity.Role;
import com.spacedvocab.entity.User;

import java.time.Instant;

/**
 * The current user's profile. {@code onboardingCompleted = false} → the frontend opens D6 Onboarding.
 */
public record UserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        int dailyNewLimit,
        String timezone,
        boolean onboardingCompleted,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getDailyNewLimit(),
                user.getTimezone().getId(),
                user.isOnboardingCompleted(),
                user.getCreatedAt());
    }
}
