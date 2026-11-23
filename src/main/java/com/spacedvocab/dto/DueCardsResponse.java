package com.spacedvocab.dto;

import java.time.Instant;
import java.util.List;

/**
 * D7 / D13: what is left to study today.
 *
 * @param reviewCount cards due today (already seen before)
 * @param newCount    new cards still allowed today (R7: capped by the daily new-card limit)
 * @param nextDueAt   only when nothing is left today: when the next card becomes due, or null if never
 * @param cards       due cards first, then new ones, at most {@code limit}
 */
public record DueCardsResponse(long reviewCount, long newCount, Instant nextDueAt, List<DueCardResponse> cards) {
}
