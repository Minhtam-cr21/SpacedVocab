package com.spacedvocab.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** D13: grade one card. The UI buttons send 1 / 3 / 4 / 5, the API accepts 0–5 (R5). */
public record ReviewRequest(
        @NotNull(message = "Hãy chọn thẻ.")
        Long cardId,

        @NotNull(message = "Hãy chấm điểm từ 0 đến 5.")
        @Min(value = 0, message = "Hãy chấm điểm từ 0 đến 5.")
        @Max(value = 5, message = "Hãy chấm điểm từ 0 đến 5.")
        Integer quality) {
}
