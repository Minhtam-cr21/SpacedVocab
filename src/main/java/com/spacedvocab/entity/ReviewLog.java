package com.spacedvocab.entity;

import com.spacedvocab.algorithm.Quality;
import com.spacedvocab.algorithm.SchedulingState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * One review, never changed or deleted (R8; a database trigger enforces it too).
 * {@code userId} / {@code cardId} are copied from the progress so daily statistics need no join.
 * Created only by {@link CardProgress#applyReview}.
 */
@Entity
@Immutable
@Table(name = "review_logs")
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_progress_id", nullable = false, updatable = false)
    private CardProgress cardProgress;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "card_id", nullable = false, updatable = false)
    private Long cardId;

    @Column(nullable = false, updatable = false)
    private int quality;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_before", nullable = false, updatable = false, length = 20)
    private CardState stateBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_after", nullable = false, updatable = false, length = 20)
    private CardState stateAfter;

    @Column(name = "ef_before", nullable = false, updatable = false)
    private double efBefore;

    @Column(name = "ef_after", nullable = false, updatable = false)
    private double efAfter;

    @Column(name = "interval_before", nullable = false, updatable = false)
    private int intervalBefore;

    @Column(name = "interval_after", nullable = false, updatable = false)
    private int intervalAfter;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt;

    protected ReviewLog() {
        // for JPA
    }

    /** Reads the "after" values from {@code progress}, which has just been updated. */
    ReviewLog(CardProgress progress, Quality quality, CardState stateBefore, SchedulingState before, Instant reviewedAt) {
        this.cardProgress = progress;
        this.userId = progress.getUser().getId();
        this.cardId = progress.getCard().getId();
        this.quality = quality.value();
        this.stateBefore = stateBefore;
        this.stateAfter = progress.getState();
        this.efBefore = before.easinessFactor().value();
        this.efAfter = progress.getEasinessFactor();
        this.intervalBefore = before.intervalDays();
        this.intervalAfter = progress.getIntervalDays();
        this.reviewedAt = reviewedAt;
    }

    public Long getId() {
        return id;
    }

    public CardProgress getCardProgress() {
        return cardProgress;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCardId() {
        return cardId;
    }

    public int getQuality() {
        return quality;
    }

    public CardState getStateBefore() {
        return stateBefore;
    }

    public CardState getStateAfter() {
        return stateAfter;
    }

    public double getEfBefore() {
        return efBefore;
    }

    public double getEfAfter() {
        return efAfter;
    }

    public int getIntervalBefore() {
        return intervalBefore;
    }

    public int getIntervalAfter() {
        return intervalAfter;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
