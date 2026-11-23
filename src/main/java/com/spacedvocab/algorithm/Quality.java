package com.spacedvocab.algorithm;

/**
 * Value object: how well the learner recalled a card, 0 (blackout) .. 5 (perfect). R5.
 */
public record Quality(int value) {

    public static final int MIN = 0;
    public static final int MAX = 5;

    public Quality {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException("quality must be in [" + MIN + ".." + MAX + "] but was " + value);
        }
    }

    public static Quality of(int value) {
        return new Quality(value);
    }
}
