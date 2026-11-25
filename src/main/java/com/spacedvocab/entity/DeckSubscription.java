package com.spacedvocab.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/** UC-L7: a learner studies a PUBLIC / OFFICIAL deck they do not own (R4, R6). */
@Entity
@Table(name = "deck_subscriptions")
public class DeckSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false, updatable = false)
    private Deck deck;

    protected DeckSubscription() {
        // for JPA
    }

    /** R4: only PUBLIC / OFFICIAL decks of someone else. */
    public DeckSubscription(User user, Deck deck) {
        this.user = Objects.requireNonNull(user, "user");
        this.deck = Objects.requireNonNull(deck, "deck");
        if (deck.getVisibility() == DeckVisibility.PRIVATE || deck.isOwnedBy(user.getId())) {
            throw new IllegalArgumentException("only another user's PUBLIC / OFFICIAL deck can be subscribed (R4)");
        }
    }

    public User getUser() {
        return user;
    }

    public Deck getDeck() {
        return deck;
    }
}
