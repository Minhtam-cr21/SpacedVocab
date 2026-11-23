package com.spacedvocab.config.security;

import com.spacedvocab.entity.Role;

import java.security.Principal;

/**
 * The logged-in user, available in controllers via {@code @AuthenticationPrincipal AuthenticatedUser me}.
 */
public record AuthenticatedUser(Long id, String email, Role role) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
