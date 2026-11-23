package com.spacedvocab.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT settings ({@code app.jwt.*}). The secret comes from the {@code JWT_SECRET} environment variable;
 * the app refuses to start without one (HS256 needs at least 256 bits).
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank(message = "Set the JWT_SECRET environment variable (see .env.example)")
        @Size(min = 32, message = "JWT_SECRET must be at least 32 characters")
        String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl) {
}
