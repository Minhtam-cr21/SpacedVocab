package com.spacedvocab.repository;

import com.spacedvocab.entity.CardState;

/** Projection: number of cards in one {@link CardState}. */
public record StateCount(CardState state, long count) {
}
