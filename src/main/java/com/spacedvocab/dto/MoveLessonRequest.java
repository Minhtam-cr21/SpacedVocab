package com.spacedvocab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** D26: change the order of a lesson inside its stage. */
public record MoveLessonRequest(
        @NotNull(message = "Thiếu thứ tự bài.") @Min(value = 1, message = "Thứ tự bài từ 1 trở lên.") Integer position) {
}
