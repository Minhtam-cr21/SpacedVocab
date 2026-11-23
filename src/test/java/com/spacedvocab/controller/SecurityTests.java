package com.spacedvocab.controller;

import com.jayway.jsonpath.JsonPath;
import com.spacedvocab.IntegrationTest;
import com.spacedvocab.entity.User;
import com.spacedvocab.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Route protection and token handling, independent of any single controller. */
@IntegrationTest
class SecurityTests extends ApiTestSupport {

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    UserRepository userRepository;

    @Test
    void healthIsPublic() {
        assertThat(mvc.get().uri("/actuator/health").exchange()).hasStatusOk();
    }

    @Test
    void missingTokenIsUnauthorized() {
        MvcTestResult result = mvc.get().uri("/api/me").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void malformedTokenIsUnauthorized() {
        MvcTestResult result = getWithToken("/api/me", "not-a-jwt");

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void expiredTokenSaysTokenExpired() {
        Session session = registerNew();
        Instant past = Instant.now().minus(Duration.ofHours(1));

        MvcTestResult result = getWithToken("/api/me", token(userIdOf(session), "spacedvocab", past));

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        Session session = registerNew();

        MvcTestResult result = getWithToken("/api/me", token(userIdOf(session), "someone-else", Instant.now()));

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void lockingAnAccountBlocksItsExistingAccessToken_R1() {
        Session session = registerNew();
        User user = userRepository.findByEmail(session.email()).orElseThrow();
        user.lock();
        userRepository.save(user);

        MvcTestResult result = getWithToken("/api/me", session.accessToken());

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    void learnerCannotReachAdminRoutes() {
        Session learner = registerNew();

        MvcTestResult result = getWithToken("/api/admin/users", learner.accessToken());

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
    }

    @Test
    void adminPassesTheAdminRouteGuard() {
        String adminToken = toSession("admin@test.local", login("admin@test.local", "admin-test-password"))
                .accessToken();

        // no admin endpoint exists before M4: getting past security means a 404, not a 403
        MvcTestResult result = getWithToken("/api/admin/users", adminToken);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
    }

    private Long userIdOf(Session session) {
        String body = body(getWithToken("/api/me", session.accessToken()));
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String token(Long userId, String issuer, Instant issuedAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
