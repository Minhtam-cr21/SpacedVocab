package com.spacedvocab.config;

import com.spacedvocab.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN from {@code ADMIN_EMAIL} / {@code ADMIN_PASSWORD} on startup.
 * Admin credentials never live in a migration or in git.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AdminProperties properties;
    private final UserService userService;

    public AdminAccountInitializer(AdminProperties properties, UserService userService) {
        this.properties = properties;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set; skipping admin bootstrap");
            return;
        }
        if (properties.password().length() < MIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_PASSWORD is shorter than {} characters; admin account not created", MIN_PASSWORD_LENGTH);
            return;
        }
        if (userService.createAdminIfAbsent(properties.email(), properties.password())) {
            log.info("Created admin account {}", properties.email());
        }
    }
}
