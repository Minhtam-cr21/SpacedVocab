package com.spacedvocab.dto;

import com.spacedvocab.entity.CardProgress;
import com.spacedvocab.entity.CardState;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The new schedule of a card after one review (UC-L9).
 *
 * @param nextReviewDate the day of the next review in the learner's timezone
 */
public record ReviewResponse(
        Long cardId,
        int quality,
        CardState state,
        double easinessFactor,
        int repetitions,
        int intervalDays,
        int lapses,
        Instant nextReviewAt,
        LocalDate nextReviewDate) {

    public static ReviewResponse of(CardProgress progress, int quality) {
        return new ReviewResponse(
                progress.getCard().getId(),
                quality,
                progress.getState(),
                Math.round(progress.getEasinessFactor() * 100) / 100.0,
                progress.getRepetitions(),
                progress.getIntervalDays(),
                progress.getLapses(),
                progress.getNextReviewAt(),
                progress.nextReviewDate());
    }
}
