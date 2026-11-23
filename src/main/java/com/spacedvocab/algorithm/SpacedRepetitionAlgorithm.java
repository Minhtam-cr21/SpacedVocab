package com.spacedvocab.algorithm;

/**
 * Strategy: computes the next scheduling state after one review.
 * Implementations are pure functions (no clock, no database) so they are easy to unit test.
 */
public interface SpacedRepetitionAlgorithm {

    /**
     * @param current state before the review ({@link SchedulingState#INITIAL} for a new card)
     * @param quality how well the learner recalled the card
     * @return state after the review; its {@code intervalDays} is always >= 1 (R5)
     */
    SchedulingState next(SchedulingState current, Quality quality);
}
