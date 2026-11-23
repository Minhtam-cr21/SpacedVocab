package com.spacedvocab.dto;

/**
 * Returned by register, login and refresh.
 *
 * @param expiresIn lifetime of {@code accessToken} in seconds
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserResponse user) {
}
