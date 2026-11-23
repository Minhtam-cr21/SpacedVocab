package com.spacedvocab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** D16, form "Đổi mật khẩu" (UC-L2). */
public record ChangePasswordRequest(
        @NotBlank(message = "Hãy nhập mật khẩu hiện tại.")
        String currentPassword,

        @NotNull(message = "Mật khẩu cần ít nhất 8 ký tự.")
        @Size(min = 8, message = "Mật khẩu cần ít nhất 8 ký tự.")
        @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự.")
        String newPassword) {
}
