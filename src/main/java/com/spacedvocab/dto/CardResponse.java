package com.spacedvocab.dto;

import com.spacedvocab.entity.Card;
import com.spacedvocab.entity.CardContent;

import java.time.Instant;

public record CardResponse(
        Long id,
        Long deckId,
        String word,
        String meaning,
        String ipa,
        String partOfSpeech,
        String example,
        String note,
        Instant createdAt,
        Instant updatedAt) {

    public static CardResponse from(Card card) {
        CardContent content = card.getContent();
        return new CardResponse(
                card.getId(),
                card.getDeck().getId(),
                content.word(),
                content.meaning(),
                content.ipa(),
                content.partOfSpeech(),
                content.example(),
                content.note(),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }
}
