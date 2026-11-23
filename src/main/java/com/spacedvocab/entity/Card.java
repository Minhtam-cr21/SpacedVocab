package com.spacedvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Shared content of a flashcard. Each learner's progress lives in {@code CardProgress} (M3), never here.
 * Deleting is soft so that review history keeps pointing at an existing card (R8).
 */
@Entity
@Table(name = "cards")
public class Card extends BaseEntity implements Ownable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false, updatable = false)
    private Deck deck;

    @Embedded
    private CardContent content;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Card() {
        // for JPA
    }

    public Card(Deck deck, CardContent content) {
        this.deck = Objects.requireNonNull(deck, "deck");
        this.content = Objects.requireNonNull(content, "content");
    }

    public void edit(CardContent content) {
        this.content = Objects.requireNonNull(content, "content");
    }

    public void delete(Instant now) {
        if (deletedAt == null) {
            deletedAt = Objects.requireNonNull(now, "now");
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** A card belongs to the owner of its deck. */
    @Override
    public boolean isOwnedBy(Long userId) {
        return deck.isOwnedBy(userId);
    }

    public Deck getDeck() {
        return deck;
    }

    public CardContent getContent() {
        return content;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
