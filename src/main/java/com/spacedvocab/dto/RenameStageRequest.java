package com.spacedvocab.dto;

import com.spacedvocab.entity.PathStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** D26: rename a stage (its CEFR level and order are fixed). */
public record RenameStageRequest(
        @NotBlank(message = "Hãy nhập tên chặng.")
        @Size(max = PathStage.MAX_TITLE_LENGTH, message = "Tên chặng tối đa 100 ký tự.") String title) {
}
