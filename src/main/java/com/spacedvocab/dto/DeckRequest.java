package com.spacedvocab.dto;

import com.spacedvocab.entity.DeckVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** D9 create / edit deck. Learners may only choose PRIVATE or PUBLIC (R3). */
public record DeckRequest(
        @NotBlank(message = "Hãy nhập tên deck.")
        @Size(max = 100, message = "Tên deck tối đa 100 ký tự.")
        String name,

        @Size(max = 500, message = "Mô tả tối đa 500 ký tự.")
        String description,

        @NotNull(message = "Hãy chọn chế độ hiển thị.")
        DeckVisibility visibility) {

    public DeckRequest {
        name = name == null ? null : name.trim();
    }
}
