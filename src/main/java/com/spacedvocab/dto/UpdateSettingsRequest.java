package com.spacedvocab.dto;

import com.spacedvocab.dto.validation.ValidTimeZone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** D6 Onboarding and D16, form "Học tập" (UC-L3, R7). */
public record UpdateSettingsRequest(
        @NotNull(message = "Hãy nhập số từ 0 đến 200.")
        @Min(value = 0, message = "Hãy nhập số từ 0 đến 200.")
        @Max(value = 200, message = "Hãy nhập số từ 0 đến 200.")
        Integer dailyNewLimit,

        @NotBlank(message = "Hãy chọn múi giờ.")
        @ValidTimeZone
        String timezone) {
}
