package com.spacedvocab.dto;

import com.spacedvocab.dto.validation.ValidTimeZone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * D2. {@code displayName} defaults to the part of the email before "@";
 * {@code timezone} is guessed by the browser and defaults to Asia/Ho_Chi_Minh.
 */
public record RegisterRequest(
        @NotBlank(message = "Hãy nhập email.")
        @Email(message = "Email chưa đúng định dạng.")
        @Size(max = 254, message = "Email tối đa 254 ký tự.")
        String email,

        @NotNull(message = "Mật khẩu cần ít nhất 8 ký tự.")
        @Size(min = 8, message = "Mật khẩu cần ít nhất 8 ký tự.")
        @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự.")
        String password,

        @Size(max = 50, message = "Tên hiển thị tối đa 50 ký tự.")
        String displayName,

        @ValidTimeZone
        String timezone) {

    /** Surrounding spaces (e.g. pasted emails) are trimmed before validation. */
    public RegisterRequest {
        email = email == null ? null : email.trim();
    }
}
