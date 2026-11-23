package com.spacedvocab.dto;

import jakarta.validation.constraints.NotBlank;

/** D3. */
public record LoginRequest(
        @NotBlank(message = "Hãy nhập email.")
        String email,

        @NotBlank(message = "Hãy nhập mật khẩu.")
        String password) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
