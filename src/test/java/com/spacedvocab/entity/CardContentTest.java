package com.spacedvocab.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CardContentTest {

    @Test
    void trimsTextAndTurnsBlankOptionalFieldsIntoNull() {
        CardContent content = new CardContent("  Abandon ", " từ bỏ ", " ", "verb", "", null);

        assertThat(content.word()).isEqualTo("Abandon");
        assertThat(content.meaning()).isEqualTo("từ bỏ");
        assertThat(content.ipa()).isNull();
        assertThat(content.partOfSpeech()).isEqualTo("verb");
        assertThat(content.example()).isNull();
        assertThat(content.normalizedWord()).isEqualTo("abandon");
    }

    @Test
    void requiresWordAndMeaning() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CardContent(" ", "nghĩa", null, null, null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new CardContent("word", null, null, null, null, null));
    }
}
