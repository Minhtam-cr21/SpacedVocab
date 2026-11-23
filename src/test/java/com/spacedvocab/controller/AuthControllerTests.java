package com.spacedvocab.controller;

import com.spacedvocab.IntegrationTest;
import com.spacedvocab.entity.User;
import com.spacedvocab.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AuthControllerTests extends ApiTestSupport {

    @Autowired
    UserRepository userRepository;

    @Nested
    class Register {

        @Test
        void createsLearnerAndLogsIn() {
            MvcTestResult result = postJson("/api/auth/register", """
                    {"email": "  Minh.Anh.%s@Example.com ", "password": "password123", "displayName": "Minh Anh",
                     "timezone": "Europe/London"}
                    """.formatted(System.nanoTime()));

            assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson()
                    .hasPathSatisfying("$.accessToken", token -> token.assertThat().asString().isNotBlank())
                    .hasPathSatisfying("$.refreshToken", token -> token.assertThat().asString().isNotBlank());
            assertThat(result).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
            assertThat(result).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
            assertThat(result).bodyJson().extractingPath("$.user.role").isEqualTo("LEARNER");
            assertThat(result).bodyJson().extractingPath("$.user.displayName").isEqualTo("Minh Anh");
            assertThat(result).bodyJson().extractingPath("$.user.timezone").isEqualTo("Europe/London");
            assertThat(result).bodyJson().extractingPath("$.user.onboardingCompleted").isEqualTo(false);
            assertThat(result).bodyJson().extractingPath("$.user.email").asString()
                    .startsWith("minh.anh.").endsWith("@example.com");
        }

        @Test
        void defaultsDisplayNameAndTimezone() {
            String email = uniqueEmail();
            Session session = register(email, PASSWORD);

            MvcTestResult me = getWithToken("/api/me", session.accessToken());

            assertThat(me).hasStatusOk();
            assertThat(me).bodyJson().extractingPath("$.displayName").isEqualTo(email.substring(0, email.indexOf('@')));
            assertThat(me).bodyJson().extractingPath("$.timezone").isEqualTo("Asia/Ho_Chi_Minh");
            assertThat(me).bodyJson().extractingPath("$.dailyNewLimit").isEqualTo(20);
        }

        @Test
        void rejectsDuplicateEmailIgnoringCase_R1() {
            String email = uniqueEmail();
            register(email, PASSWORD);

            MvcTestResult result = postJson("/api/auth/register", """
                    {"email": "%s", "password": "password123"}
                    """.formatted(email.toUpperCase()));

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("EMAIL_TAKEN");
            assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("email");
        }

        @Test
        void rejectsShortPasswordAndInvalidEmail_R1() {
            MvcTestResult result = postJson("/api/auth/register", """
                    {"email": "not-an-email", "password": "1234567"}
                    """);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
            assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
                    .containsExactlyInAnyOrder("email", "password");
            assertThat(body(result)).contains("Mật khẩu cần ít nhất 8 ký tự.");
        }

        @Test
        void rejectsUnknownTimezone() {
            MvcTestResult result = postJson("/api/auth/register", """
                    {"email": "%s", "password": "password123", "timezone": "Mars/Olympus"}
                    """.formatted(uniqueEmail()));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("timezone");
        }

        @Test
        void rejectsMalformedJson() {
            MvcTestResult result = postJson("/api/auth/register", "{not json");

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        }
    }

    @Nested
    class Login {

        @Test
        void succeedsWithEmailInAnyCase() {
            String email = uniqueEmail();
            register(email, PASSWORD);

            MvcTestResult result = login(email.toUpperCase(), PASSWORD);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.user.email").isEqualTo(email);
        }

        @Test
        void wrongPasswordAndUnknownEmailGiveTheSameError() {
            String email = uniqueEmail();
            register(email, PASSWORD);

            MvcTestResult wrongPassword = login(email, "wrong-password");
            MvcTestResult unknownEmail = login(uniqueEmail(), PASSWORD);

            for (MvcTestResult result : new MvcTestResult[] {wrongPassword, unknownEmail}) {
                assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
                assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAD_CREDENTIALS");
            }
        }

        @Test
        void lockedAccountCannotLogIn_R1() {
            String email = uniqueEmail();
            register(email, PASSWORD);
            lock(email);

            MvcTestResult result = login(email, PASSWORD);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCOUNT_LOCKED");
        }

        @Test
        void lockedAccountWithWrongPasswordDoesNotRevealTheLock() {
            String email = uniqueEmail();
            register(email, PASSWORD);
            lock(email);

            assertThat(login(email, "wrong-password")).bodyJson().extractingPath("$.code").isEqualTo("BAD_CREDENTIALS");
        }

        @Test
        void bootstrappedAdminCanLogIn() {
            MvcTestResult result = login("admin@test.local", "admin-test-password");

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.user.role").isEqualTo("ADMIN");
            assertThat(result).bodyJson().extractingPath("$.user.onboardingCompleted").isEqualTo(true);
        }
    }

    @Nested
    class Refresh {

        @Test
        void rotatesTheRefreshToken() {
            Session session = registerNew();

            MvcTestResult result = refresh(session.refreshToken());

            assertThat(result).hasStatusOk();
            Session rotated = toSession(session.email(), result);
            assertThat(rotated.refreshToken()).isNotEqualTo(session.refreshToken());
            assertThat(getWithToken("/api/me", rotated.accessToken())).hasStatusOk();
        }

        @Test
        void reusingARotatedTokenEndsAllSessions() {
            Session session = registerNew();
            Session rotated = toSession(session.email(), refresh(session.refreshToken()));

            MvcTestResult reuse = refresh(session.refreshToken());

            assertThat(reuse).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(reuse).bodyJson().extractingPath("$.code").isEqualTo("TOKEN_EXPIRED");
            // the legitimate newer token was revoked too
            assertThat(refresh(rotated.refreshToken())).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void rejectsUnknownToken() {
            MvcTestResult result = refresh("does-not-exist");

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("TOKEN_EXPIRED");
        }

        @Test
        void lockedAccountCannotRefresh_R1() {
            Session session = registerNew();
            lock(session.email());

            MvcTestResult result = refresh(session.refreshToken());

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCOUNT_LOCKED");
        }

        @Test
        void ignoresAStaleAccessTokenHeader() {
            Session session = registerNew();

            MvcTestResult result = mvc.post().uri("/api/auth/refresh")
                    .header("Authorization", "Bearer garbage")
                    .contentType("application/json")
                    .content("{\"refreshToken\": \"%s\"}".formatted(session.refreshToken()))
                    .exchange();

            assertThat(result).hasStatusOk();
        }
    }

    @Nested
    class Logout {

        @Test
        void revokesTheRefreshToken() {
            Session session = registerNew();

            MvcTestResult result = postJson("/api/auth/logout",
                    "{\"refreshToken\": \"%s\"}".formatted(session.refreshToken()));

            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(refresh(session.refreshToken())).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void isIdempotent() {
            assertThat(postJson("/api/auth/logout", "{\"refreshToken\": \"unknown\"}"))
                    .hasStatus(HttpStatus.NO_CONTENT);
        }
    }

    private void lock(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.lock();
        userRepository.save(user);
    }
}
