package com.spacedvocab.repository;

import com.spacedvocab.entity.Deck;

/** Query projection: a deck plus the number of its cards that are not deleted. */
public record DeckWithCardCount(Deck deck, long cardCount) {
}
