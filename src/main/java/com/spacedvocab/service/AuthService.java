package com.spacedvocab.service;

import com.spacedvocab.dto.AuthResponse;
import com.spacedvocab.dto.LoginRequest;
import com.spacedvocab.dto.RegisterRequest;
import com.spacedvocab.entity.RefreshToken;
import com.spacedvocab.entity.User;
import com.spacedvocab.exception.AppException;
import com.spacedvocab.exception.DuplicateResourceException;
import com.spacedvocab.exception.ErrorCode;
import com.spacedvocab.exception.ForbiddenOperationException;
import com.spacedvocab.exception.UnauthorizedException;
import com.spacedvocab.repository.RefreshTokenRepository;
import com.spacedvocab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

/** UC-G1 register, UC-G2 login, plus refresh-token rotation and logout. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_DISPLAY_NAME = 50;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    /** Compared against when the email is unknown, so both cases take about the same time. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       TokenService tokenService, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("timing-attack-dummy-password");
    }

    /** R1: email unique (case-insensitive), password ≥ 8 (validated in the DTO). Logs the user in. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(ErrorCode.EMAIL_TAKEN, "email");
        }
        ZoneId timezone = request.timezone() != null ? ZoneId.of(request.timezone()) : User.DEFAULT_TIMEZONE;
        User user = User.newLearner(email, passwordEncoder.encode(request.password()),
                displayNameOrDefault(request.displayName(), email), timezone);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // two registrations with the same email at the same time: the unique constraint wins
            throw new DuplicateResourceException(ErrorCode.EMAIL_TAKEN, "email");
        }
        log.info("User {} registered", user.getId());
        return tokenService.issueTokens(user);
    }

    /**
     * R1: a LOCKED account cannot log in. The password is checked first so a wrong password
     * never reveals whether an account exists or is locked.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(User.normalizeEmail(request.email()));
        String hash = found.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !passwordMatches) {
            throw new UnauthorizedException(ErrorCode.BAD_CREDENTIALS);
        }
        User user = found.get();
        if (user.isLocked()) {
            throw new ForbiddenOperationException(ErrorCode.ACCOUNT_LOCKED);
        }
        return tokenService.issueTokens(user);
    }

    /**
     * Rotates the refresh token: the old one is revoked and a new pair is issued.
     * Re-using an already revoked token means it was probably stolen, so every session of that user is ended.
     * {@code noRollbackFor}: those revocations must be kept even though an error is returned.
     */
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(TokenService.hash(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.TOKEN_EXPIRED));
        User user = token.getUser();
        if (token.isRevoked()) {
            int revoked = refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
            log.warn("Revoked refresh token reused for user {}; ended {} active session(s)", user.getId(), revoked);
            throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);
        }
        if (token.isExpired(now)) {
            throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);
        }
        if (user.isLocked()) {
            refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
            throw new ForbiddenOperationException(ErrorCode.ACCOUNT_LOCKED);
        }
        token.revoke(now);
        return tokenService.issueTokens(user);
    }

    /** Idempotent: an unknown or already revoked token is ignored. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenService.hash(rawRefreshToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private static String displayNameOrDefault(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        String localPart = email.substring(0, email.indexOf('@'));
        return localPart.length() > MAX_DISPLAY_NAME ? localPart.substring(0, MAX_DISPLAY_NAME) : localPart;
    }
}
