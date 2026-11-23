package com.spacedvocab.dto;

import com.spacedvocab.entity.Deck;
import com.spacedvocab.entity.DeckVisibility;

import java.time.Instant;

/**
 * A deck as shown in D8 (DeckCard) and D10 (header). {@code editable} tells the frontend whether to show
 * the owner actions (edit, add card, import). {@code progress} is the viewer's own progress; it is only
 * filled for a single deck (D10), null in lists.
 */
public record DeckResponse(
        Long id,
        String name,
        String description,
        DeckVisibility visibility,
        Owner owner,
        long cardCount,
        boolean editable,
        Progress progress,
        Instant createdAt,
        Instant updatedAt) {

    public record Owner(Long id, String displayName) {
    }

    /** D10 stat tiles. {@code newCount} = cards the viewer has never reviewed. */
    public record Progress(long newCount, long learningCount, long reviewCount) {
    }

    public static DeckResponse from(Deck deck, long cardCount, boolean editable) {
        return from(deck, cardCount, editable, null);
    }

    public static DeckResponse from(Deck deck, long cardCount, boolean editable, Progress progress) {
        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getVisibility(),
                new Owner(deck.getOwner().getId(), deck.getOwner().getDisplayName()),
                cardCount,
                editable,
                progress,
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }
}
