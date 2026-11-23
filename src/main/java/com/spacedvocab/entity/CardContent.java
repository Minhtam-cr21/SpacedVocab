package com.spacedvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Locale;

/**
 * Value object: what is written on a card. Self-validating; text is trimmed and blank optional fields become null.
 */
@Embeddable
public record CardContent(
        @Column(nullable = false, length = 100) String word,
        @Column(nullable = false, length = 500) String meaning,
        @Column(length = 100) String ipa,
        @Column(name = "part_of_speech", length = 30) String partOfSpeech,
        @Column(length = 500) String example,
        @Column(length = 500) String note) {

    public CardContent {
        word = requireText(word, "word");
        meaning = requireText(meaning, "meaning");
        ipa = Deck.blankToNull(ipa);
        partOfSpeech = Deck.blankToNull(partOfSpeech);
        example = Deck.blankToNull(example);
        note = Deck.blankToNull(note);
    }

    /** R2 compares words case-insensitively. */
    public String normalizedWord() {
        return word.toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
