package com.spacedvocab.algorithm;

import java.util.Objects;

/**
 * The part of a learner's progress that a spaced repetition algorithm reads and writes.
 *
 * @param easinessFactor current EF
 * @param repetitions    successful reviews in a row (reset to 0 when the card is forgotten)
 * @param intervalDays   days until the next review; 0 only for a card never reviewed
 */
public record SchedulingState(EasinessFactor easinessFactor, int repetitions, int intervalDays) {

    /** A card that has never been reviewed (business-spec §5). */
    public static final SchedulingState INITIAL = new SchedulingState(EasinessFactor.INITIAL, 0, 0);

    public SchedulingState {
        Objects.requireNonNull(easinessFactor, "easinessFactor");
        if (repetitions < 0) {
            throw new IllegalArgumentException("repetitions must be >= 0 but was " + repetitions);
        }
        if (intervalDays < 0) {
            throw new IllegalArgumentException("intervalDays must be >= 0 but was " + intervalDays);
        }
    }
}
