package com.spacedvocab.dto;

import com.spacedvocab.entity.Card;
import com.spacedvocab.entity.CardContent;
import com.spacedvocab.entity.CardState;

import java.util.Map;

/**
 * One card of a review session (D13). {@code previewIntervals} maps each rating button's quality
 * (1 Quên, 3 Khó, 4 Nhớ, 5 Dễ) to the interval in days it would schedule.
 */
public record DueCardResponse(
        Long cardId,
        Long deckId,
        String deckName,
        String word,
        String meaning,
        String ipa,
        String partOfSpeech,
        String example,
        String note,
        CardState state,
        Map<Integer, Integer> previewIntervals) {

    public static DueCardResponse of(Card card, CardState state, Map<Integer, Integer> previewIntervals) {
        CardContent content = card.getContent();
        return new DueCardResponse(
                card.getId(),
                card.getDeck().getId(),
                card.getDeck().getName(),
                content.word(),
                content.meaning(),
                content.ipa(),
                content.partOfSpeech(),
                content.example(),
                content.note(),
                state,
                previewIntervals);
    }
}
