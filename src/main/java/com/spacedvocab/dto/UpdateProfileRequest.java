package com.spacedvocab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** D16, form "Hồ sơ". */
public record UpdateProfileRequest(
        @NotBlank(message = "Hãy nhập tên hiển thị.")
        @Size(max = 50, message = "Tên hiển thị tối đa 50 ký tự.")
        String displayName) {
}
