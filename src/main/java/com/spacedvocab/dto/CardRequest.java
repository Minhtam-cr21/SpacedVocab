package com.spacedvocab.dto;

import com.spacedvocab.entity.CardContent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** D11 create / edit card. Only {@code word} and {@code meaning} are required. */
public record CardRequest(
        @NotBlank(message = "Hãy nhập từ.")
        @Size(max = 100, message = "Từ tối đa 100 ký tự.")
        String word,

        @NotBlank(message = "Hãy nhập nghĩa.")
        @Size(max = 500, message = "Nghĩa tối đa 500 ký tự.")
        String meaning,

        @Size(max = 100, message = "Phiên âm tối đa 100 ký tự.")
        String ipa,

        @Size(max = 30, message = "Từ loại tối đa 30 ký tự.")
        String partOfSpeech,

        @Size(max = 500, message = "Ví dụ tối đa 500 ký tự.")
        String example,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note) {

    public CardRequest {
        word = word == null ? null : word.trim();
        meaning = meaning == null ? null : meaning.trim();
    }

    public CardContent toContent() {
        return new CardContent(word, meaning, ipa, partOfSpeech, example, note);
    }
}
