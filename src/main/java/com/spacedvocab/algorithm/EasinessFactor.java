package com.spacedvocab.algorithm;

/**
 * Value object: how easy a card is for one learner. Never below {@link #MIN} (R5).
 */
public record EasinessFactor(double value) {

    public static final double MIN = 1.3;
    public static final EasinessFactor INITIAL = new EasinessFactor(2.5);

    public EasinessFactor {
        if (Double.isNaN(value) || value < MIN) {
            throw new IllegalArgumentException("easiness factor must be >= " + MIN + " but was " + value);
        }
    }

    public static EasinessFactor of(double value) {
        return new EasinessFactor(value);
    }
}
