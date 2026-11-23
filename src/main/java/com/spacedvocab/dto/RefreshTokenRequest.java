package com.spacedvocab.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/auth/refresh} and {@code POST /api/auth/logout}. */
public record RefreshTokenRequest(
        @NotBlank(message = "Thiếu refresh token.")
        String refreshToken) {
}
