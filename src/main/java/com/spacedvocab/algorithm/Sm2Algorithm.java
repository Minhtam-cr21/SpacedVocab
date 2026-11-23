package com.spacedvocab.algorithm;

import org.springframework.stereotype.Component;

/**
 * SM-2 (Woźniak, 1987). Formula: docs/domain/business-spec.md §5.
 *
 * <p>M3 tutor mode: this class and its test are written by the project owner.
 */
@Component
public class Sm2Algorithm implements SpacedRepetitionAlgorithm {

    @Override
    public SchedulingState next(SchedulingState current, Quality quality) {
        // TODO(M3): implement SM-2
        throw new UnsupportedOperationException("SM-2 not implemented yet");
    }
}
