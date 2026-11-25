package com.spacedvocab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** D26: add an OFFICIAL deck to a stage as a lesson (R10). */
public record AddLessonRequest(
        @NotNull(message = "Hãy chọn deck.") Long deckId,
        @NotNull(message = "Thiếu thứ tự bài.") @Min(value = 1, message = "Thứ tự bài từ 1 trở lên.") Integer position) {
}
