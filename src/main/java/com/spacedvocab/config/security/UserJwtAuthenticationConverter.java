package com.spacedvocab.config.security;

import com.spacedvocab.entity.User;
import com.spacedvocab.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Turns a valid access token into an authenticated {@link AuthenticatedUser}.
 * The user is re-read on every request, so locking an account (R1) or changing a role
 * takes effect immediately instead of when the token expires.
 */
@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public UserJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = parseUserId(jwt.getSubject())
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new InvalidBearerTokenException("Token subject is not an existing user"));
        if (user.isLocked()) {
            throw new LockedException("Account is locked");
        }
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, authorities);
    }

    private static Optional<Long> parseUserId(String subject) {
        try {
            return Optional.of(Long.valueOf(subject));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
