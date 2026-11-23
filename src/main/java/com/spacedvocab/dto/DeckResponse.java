package com.spacedvocab.dto;

import com.spacedvocab.entity.Deck;
import com.spacedvocab.entity.DeckVisibility;

import java.time.Instant;

/**
 * A deck as shown in D8 (DeckCard) and D10 (header). {@code editable} tells the frontend whether to show
 * the owner actions (edit, add card, import).
 */
public record DeckResponse(
        Long id,
        String name,
        String description,
        DeckVisibility visibility,
        Owner owner,
        long cardCount,
        boolean editable,
        Instant createdAt,
        Instant updatedAt) {

    public record Owner(Long id, String displayName) {
    }

    public static DeckResponse from(Deck deck, long cardCount, boolean editable) {
        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getVisibility(),
                new Owner(deck.getOwner().getId(), deck.getOwner().getDisplayName()),
                cardCount,
                editable,
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }
}
