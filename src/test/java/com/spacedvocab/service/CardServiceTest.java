package com.spacedvocab.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CardServiceTest {

    @ParameterizedTest(name = "[{0}] → [{1}]")
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "\"\"     | %",
            "\"   \"  | %",
            "Run      | %run%",
            "100%     | %100\\%%",
            "snake_ca | %snake\\_ca%",
            "a\\b     | %a\\\\b%"
    })
    void buildsEscapedLikePattern(String query, String expected) {
        assertThat(CardService.likePattern(query)).isEqualTo(expected);
    }
}
