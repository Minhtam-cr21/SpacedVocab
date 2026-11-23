package com.spacedvocab.entity;

/** Learning state of a card for one learner (business-spec §5). */
public enum CardState {
    /** Never reviewed. */
    NEW,
    /** In the first steps (repetitions &lt; 2) or just forgotten. */
    LEARNING,
    /** Remembered at least twice in a row. */
    REVIEW;

    static final int REVIEW_AFTER_REPETITIONS = 2;

    static CardState afterReview(int repetitions) {
        return repetitions >= REVIEW_AFTER_REPETITIONS ? REVIEW : LEARNING;
    }
}
