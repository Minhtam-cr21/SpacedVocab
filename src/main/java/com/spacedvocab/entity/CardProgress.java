package com.spacedvocab.entity;

import com.spacedvocab.algorithm.EasinessFactor;
import com.spacedvocab.algorithm.Quality;
import com.spacedvocab.algorithm.SchedulingState;
import com.spacedvocab.algorithm.SpacedRepetitionAlgorithm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * One learner's progress on one card (rich domain model). The scheduling fields have no setters:
 * they only change through {@link #applyReview}, which also produces the matching {@link ReviewLog}.
 */
@Entity
@Table(name = "card_progress")
public class CardProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false, updatable = false)
    private Card card;

    @Column(name = "easiness_factor", nullable = false)
    private double easinessFactor;

    @Column(nullable = false)
    private int repetitions;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardState state;

    @Column(nullable = false)
    private int lapses;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    protected CardProgress() {
        // for JPA
    }

    /** Progress of a card the learner has never reviewed. */
    public CardProgress(User user, Card card) {
        this.user = Objects.requireNonNull(user, "user");
        this.card = Objects.requireNonNull(card, "card");
        SchedulingState initial = SchedulingState.INITIAL;
        this.easinessFactor = initial.easinessFactor().value();
        this.repetitions = initial.repetitions();
        this.intervalDays = initial.intervalDays();
        this.state = CardState.NEW;
    }

    /**
     * UC-S1: run the algorithm for one review. The next review is at the start of
     * "today + interval" in the learner's timezone (R7), so a card is due for the whole day.
     *
     * @return the immutable log entry to store (R8)
     */
    public ReviewLog applyReview(Quality quality, SpacedRepetitionAlgorithm algorithm, Clock clock) {
        Objects.requireNonNull(quality, "quality");
        SchedulingState before = schedulingState();
        CardState stateBefore = state;

        SchedulingState after = algorithm.next(before, quality);
        if (after.intervalDays() < 1) {
            throw new IllegalStateException("algorithm returned interval " + after.intervalDays() + " (R5: >= 1)");
        }

        Instant now = clock.instant();
        ZoneId zone = user.getTimezone();
        LocalDate today = LocalDate.ofInstant(now, zone);

        if (before.repetitions() > 0 && after.repetitions() == 0) {
            lapses++; // forgotten after having been remembered
        }
        easinessFactor = after.easinessFactor().value();
        repetitions = after.repetitions();
        intervalDays = after.intervalDays();
        state = CardState.afterReview(repetitions);
        nextReviewAt = today.plusDays(intervalDays).atStartOfDay(zone).toInstant();
        lastReviewedAt = now;

        return new ReviewLog(this, quality, stateBefore, before, now);
    }

    public SchedulingState schedulingState() {
        return new SchedulingState(EasinessFactor.of(easinessFactor), repetitions, intervalDays);
    }

    /** Day of the next review in the learner's timezone, or null for a NEW card. */
    public LocalDate nextReviewDate() {
        return nextReviewAt == null ? null : LocalDate.ofInstant(nextReviewAt, user.getTimezone());
    }

    public User getUser() {
        return user;
    }

    public Card getCard() {
        return card;
    }

    public double getEasinessFactor() {
        return easinessFactor;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public CardState getState() {
        return state;
    }

    public int getLapses() {
        return lapses;
    }

    public Instant getNextReviewAt() {
        return nextReviewAt;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }
}
