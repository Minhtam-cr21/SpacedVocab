package com.spacedvocab.controller;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.ChangePasswordRequest;
import com.spacedvocab.dto.UpdateProfileRequest;
import com.spacedvocab.dto.UpdateSettingsRequest;
import com.spacedvocab.dto.UserResponse;
import com.spacedvocab.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** D6 Onboarding and D16 Settings: the current user's own account. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        return userService.getProfile(me.id());
    }

    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser me,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(me.id(), request);
    }

    @PutMapping("/settings")
    public UserResponse updateSettings(@AuthenticationPrincipal AuthenticatedUser me,
                                       @Valid @RequestBody UpdateSettingsRequest request) {
        return userService.updateSettings(me.id(), request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthenticatedUser me,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(me.id(), request);
    }
}
