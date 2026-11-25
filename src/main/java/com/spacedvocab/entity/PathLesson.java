package com.spacedvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/** A lesson of a stage. Its content is one OFFICIAL deck (R10). */
@Entity
@Table(name = "path_lessons")
public class PathLesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false, updatable = false)
    private PathStage stage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false, updatable = false)
    private Deck deck;

    @Column(nullable = false)
    private int position;

    protected PathLesson() {
        // for JPA
    }

    public PathLesson(PathStage stage, Deck deck, int position) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.deck = Objects.requireNonNull(deck, "deck");
        if (deck.getVisibility() != DeckVisibility.OFFICIAL || deck.isDeleted()) {
            throw new IllegalArgumentException("a lesson must be an OFFICIAL deck (R10)");
        }
        moveTo(position);
    }

    public void moveTo(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("position must be >= 1");
        }
        this.position = position;
    }

    public PathStage getStage() {
        return stage;
    }

    public Deck getDeck() {
        return deck;
    }

    public int getPosition() {
        return position;
    }
}
