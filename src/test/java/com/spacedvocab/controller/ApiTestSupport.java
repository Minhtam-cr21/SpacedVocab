package com.spacedvocab.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Small helpers shared by the API integration tests. Every test uses fresh, unique emails. */
abstract class ApiTestSupport {

    static final String PASSWORD = "password123";

    @Autowired
    MockMvcTester mvc;

    record Session(String email, String accessToken, String refreshToken) {
    }

    static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    MvcTestResult postJson(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    MvcTestResult putJson(String uri, String accessToken, String json) {
        return mvc.put().uri(uri).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    MvcTestResult getWithToken(String uri, String accessToken) {
        return mvc.get().uri(uri).header("Authorization", "Bearer " + accessToken).exchange();
    }

    Session register(String email, String password) {
        MvcTestResult result = postJson("/api/auth/register",
                """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return toSession(email, result);
    }

    Session registerNew() {
        return register(uniqueEmail(), PASSWORD);
    }

    MvcTestResult login(String email, String password) {
        return postJson("/api/auth/login",
                """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }

    MvcTestResult refresh(String refreshToken) {
        return postJson("/api/auth/refresh", """
                {"refreshToken": "%s"}
                """.formatted(refreshToken));
    }

    static Session toSession(String email, MvcTestResult result) {
        String body = body(result);
        return new Session(email, JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    static String body(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
