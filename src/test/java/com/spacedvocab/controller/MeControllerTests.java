package com.spacedvocab.controller;

import com.spacedvocab.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class MeControllerTests extends ApiTestSupport {

    @Test
    void updatesProfile() {
        Session session = registerNew();

        MvcTestResult result = putJson("/api/me", session.accessToken(), """
                {"displayName": "  Anh Tuấn  "}
                """);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.displayName").isEqualTo("Anh Tuấn");
    }

    @Test
    void rejectsBlankDisplayName() {
        Session session = registerNew();

        MvcTestResult result = putJson("/api/me", session.accessToken(), """
                {"displayName": "   "}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("displayName");
    }

    @Test
    void savingSettingsCompletesOnboarding() {
        Session session = registerNew();

        MvcTestResult result = putJson("/api/me/settings", session.accessToken(), """
                {"dailyNewLimit": 40, "timezone": "Asia/Tokyo"}
                """);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.dailyNewLimit").isEqualTo(40);
        assertThat(result).bodyJson().extractingPath("$.timezone").isEqualTo("Asia/Tokyo");
        assertThat(result).bodyJson().extractingPath("$.onboardingCompleted").isEqualTo(true);
    }

    @Test
    void rejectsSettingsOutOfRange_R7() {
        Session session = registerNew();

        MvcTestResult tooMany = putJson("/api/me/settings", session.accessToken(), """
                {"dailyNewLimit": 201, "timezone": "Asia/Tokyo"}
                """);
        MvcTestResult badZone = putJson("/api/me/settings", session.accessToken(), """
                {"dailyNewLimit": 10, "timezone": "Mars/Olympus"}
                """);

        assertThat(tooMany).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(tooMany).bodyJson().extractingPath("$.field").isEqualTo("dailyNewLimit");
        assertThat(badZone).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(badZone).bodyJson().extractingPath("$.field").isEqualTo("timezone");
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() {
        Session session = registerNew();

        MvcTestResult result = putJson("/api/me/password", session.accessToken(), """
                {"currentPassword": "wrong-password", "newPassword": "new-password-456"}
                """);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("WRONG_CURRENT_PASSWORD");
        assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("currentPassword");
    }

    @Test
    void changePasswordReplacesItAndEndsOtherSessions() {
        Session session = registerNew();

        MvcTestResult result = putJson("/api/me/password", session.accessToken(), """
                {"currentPassword": "%s", "newPassword": "new-password-456"}
                """.formatted(PASSWORD));

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(login(session.email(), PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(login(session.email(), "new-password-456")).hasStatusOk();
        assertThat(refresh(session.refreshToken())).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
