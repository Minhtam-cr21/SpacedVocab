package com.spacedvocab.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One try at the stage test (R12): 20 questions "Vietnamese meaning → type the English word",
 * 10 minutes, at least 18 correct to pass, 24 hours to wait after failing.
 */
@Entity
@Table(name = "stage_test_attempts")
public class StageTestAttempt extends BaseEntity {

    public static final int QUESTION_COUNT = 20;
    public static final int PASS_SCORE = 18;
    public static final Duration TIME_LIMIT = Duration.ofMinutes(10);
    /** Late submissions within this margin (slow network) are still graded. */
    public static final Duration SUBMIT_GRACE = Duration.ofSeconds(30);
    public static final Duration COOLDOWN = Duration.ofHours(24);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false, updatable = false)
    private PathStage stage;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column
    private Boolean passed;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<StageTestQuestion> questions = new ArrayList<>();

    protected StageTestAttempt() {
        // for JPA
    }

    public StageTestAttempt(User user, PathStage stage, List<Card> cards, Instant now) {
        this.user = Objects.requireNonNull(user, "user");
        this.stage = Objects.requireNonNull(stage, "stage");
        if (cards.size() != QUESTION_COUNT) {
            throw new IllegalArgumentException("a stage test has exactly " + QUESTION_COUNT + " questions");
        }
        this.startedAt = Objects.requireNonNull(now, "now");
        this.expiresAt = now.plus(TIME_LIMIT);
        for (int i = 0; i < cards.size(); i++) {
            questions.add(new StageTestQuestion(this, cards.get(i), i + 1));
        }
    }

    /**
     * Grades every question. Answers sent after the time limit (plus a short grace) are ignored,
     * so every question of a late submission is wrong.
     *
     * @param answers answer by question position; missing positions are unanswered
     */
    public void submit(Map<Integer, String> answers, Instant now) {
        if (isSubmitted()) {
            throw new IllegalStateException("attempt already submitted");
        }
        boolean inTime = !now.isAfter(expiresAt.plus(SUBMIT_GRACE));
        int correct = 0;
        for (StageTestQuestion question : questions) {
            question.grade(inTime ? answers.get(question.getPosition()) : null);
            if (question.isCorrect()) {
                correct++;
            }
        }
        this.submittedAt = now;
        this.correctCount = correct;
        this.passed = correct >= PASS_SCORE;
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    /** Still being answered: not submitted and within the time limit. */
    public boolean isInProgress(Instant now) {
        return !isSubmitted() && now.isBefore(expiresAt);
    }

    /** Failed attempts: submitted below the pass score, or never submitted before the time ran out. */
    public boolean isFailed(Instant now) {
        return isSubmitted() ? !passed : !now.isBefore(expiresAt);
    }

    /** When the learner may try again after failing (R12). */
    public Instant retryAt() {
        return (isSubmitted() ? submittedAt : expiresAt).plus(COOLDOWN);
    }

    public User getUser() {
        return user;
    }

    public PathStage getStage() {
        return stage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public boolean isPassed() {
        return Boolean.TRUE.equals(passed);
    }

    public List<StageTestQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }
}
