package com.spacedvocab.algorithm;

import org.springframework.stereotype.Component;

/**
 * SM-2 (Woźniak, 1987). Formula: docs/domain/business-spec.md §5.
 * <p>
 * A pure function: it only reads the current state and the quality, so it needs no clock or database.
 * Turning the interval into a date is done by {@code CardProgress#applyReview}.
 */
@Component
public class Sm2Algorithm implements SpacedRepetitionAlgorithm {

    /** Quality from which the answer counts as remembered. */
    static final int PASSING_QUALITY = 3;
    static final int FIRST_INTERVAL_DAYS = 1;
    static final int SECOND_INTERVAL_DAYS = 6;

    @Override
    public SchedulingState next(SchedulingState current, Quality quality) {
        if (quality.value() < PASSING_QUALITY) {
            // Forgotten: start the card over, but keep its EF (as in the original SM-2).
            return new SchedulingState(current.easinessFactor(), 0, FIRST_INTERVAL_DAYS);
        }

        double ef = current.easinessFactor().value();
        int interval = switch (current.repetitions()) {
            case 0 -> FIRST_INTERVAL_DAYS;
            case 1 -> SECOND_INTERVAL_DAYS;
            // the interval grows with the EF the card had before this review
            default -> (int) Math.round(current.intervalDays() * ef);
        };
        return new SchedulingState(nextEasinessFactor(ef, quality), current.repetitions() + 1, interval);
    }

    /** EF' = EF + (0.1 − (5−q) × (0.08 + (5−q) × 0.02)), never below 1.3 (R5). */
    private static EasinessFactor nextEasinessFactor(double ef, Quality quality) {
        int distance = Quality.MAX - quality.value(); // 0 for a perfect answer
        double next = ef + (0.1 - distance * (0.08 + distance * 0.02));
        return EasinessFactor.of(Math.max(EasinessFactor.MIN, next));
    }
}
