package com.spacedvocab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * First admin account ({@code app.admin.*}, from {@code ADMIN_EMAIL} / {@code ADMIN_PASSWORD}).
 * Created on startup only if both are set and the email does not exist yet.
 */
@ConfigurationProperties("app.admin")
public record AdminProperties(String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
