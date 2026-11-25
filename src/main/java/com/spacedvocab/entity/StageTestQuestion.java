package com.spacedvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;

/** One question of a stage test: the card's meaning is shown, its word is the expected answer. */
@Entity
@Table(name = "stage_test_questions")
public class StageTestQuestion extends BaseEntity {

    public static final int MAX_ANSWER_LENGTH = 100;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false, updatable = false)
    private StageTestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false, updatable = false)
    private Card card;

    @Column(nullable = false, updatable = false)
    private int position;

    @Column(length = MAX_ANSWER_LENGTH)
    private String answer;

    @Column
    private Boolean correct;

    protected StageTestQuestion() {
        // for JPA
    }

    StageTestQuestion(StageTestAttempt attempt, Card card, int position) {
        this.attempt = Objects.requireNonNull(attempt, "attempt");
        this.card = Objects.requireNonNull(card, "card");
        this.position = position;
    }

    /** R12: trimmed and case-insensitive; a blank answer is wrong. */
    void grade(String rawAnswer) {
        this.answer = rawAnswer == null || rawAnswer.isBlank() ? null : rawAnswer.trim();
        this.correct = answer != null
                && answer.toLowerCase(Locale.ROOT).equals(card.getContent().normalizedWord());
    }

    public StageTestAttempt getAttempt() {
        return attempt;
    }

    public Card getCard() {
        return card;
    }

    public int getPosition() {
        return position;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isCorrect() {
        return Boolean.TRUE.equals(correct);
    }
}
