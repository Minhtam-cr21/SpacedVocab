package com.spacedvocab.service;

import com.spacedvocab.dto.ChangePasswordRequest;
import com.spacedvocab.dto.UpdateProfileRequest;
import com.spacedvocab.dto.UpdateSettingsRequest;
import com.spacedvocab.dto.UserResponse;
import com.spacedvocab.entity.User;
import com.spacedvocab.exception.BusinessRuleViolationException;
import com.spacedvocab.exception.ErrorCode;
import com.spacedvocab.exception.ResourceNotFoundException;
import com.spacedvocab.repository.RefreshTokenRepository;
import com.spacedvocab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneId;

/** UC-L1 profile, UC-L2 password, UC-L3 study settings. */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.rename(request.displayName());
        return UserResponse.from(user);
    }

    /** Also completes onboarding the first time it is called (D6). */
    @Transactional
    public UserResponse updateSettings(Long userId, UpdateSettingsRequest request) {
        User user = findUser(userId);
        user.updateStudySettings(request.dailyNewLimit(), ZoneId.of(request.timezone()), clock.instant());
        return UserResponse.from(user);
    }

    /** Ends every other session: all refresh tokens are revoked. */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleViolationException(ErrorCode.WRONG_CURRENT_PASSWORD, "currentPassword");
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllActiveByUserId(userId, clock.instant());
        log.info("User {} changed password", userId);
    }

    /** Used by the startup bootstrap. Returns {@code true} if a new admin was created. */
    @Transactional
    public boolean createAdminIfAbsent(String email, String rawPassword) {
        String normalized = User.normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            return false;
        }
        userRepository.save(User.newAdmin(normalized, passwordEncoder.encode(rawPassword), "Admin", clock.instant()));
        return true;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(ResourceNotFoundException::new);
    }
}
