package com.spacedvocab.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** R12: grading, time limit and cooldown of a stage test. */
class StageTestAttemptTest {

    private static final Instant START = Instant.parse("2026-11-25T08:00:00Z");

    private static StageTestAttempt attempt(int questions) {
        User user = User.newLearner("learner@example.com", "{noop}x", "Learner", ZoneId.of("Asia/Ho_Chi_Minh"));
        ReflectionTestUtils.setField(user, "id", 1L);
        Deck deck = new Deck(user, "Deck", null, DeckVisibility.PRIVATE);
        List<Card> cards = new ArrayList<>();
        for (int i = 1; i <= questions; i++) {
            cards.add(new Card(deck, new CardContent("Word" + i, "nghĩa " + i, null, null, null, null)));
        }
        return new StageTestAttempt(user, new PathStage(), cards, START);
    }

    /** Correct answers for the first {@code correct} questions, wrong ones after that. */
    private static Map<Integer, String> answers(int correct) {
        Map<Integer, String> answers = new HashMap<>();
        for (int i = 1; i <= StageTestAttempt.QUESTION_COUNT; i++) {
            answers.put(i, i <= correct ? "word" + i : "nope");
        }
        return answers;
    }

    @Test
    void needsExactly20Questions() {
        assertThatIllegalArgumentException().isThrownBy(() -> attempt(19));
    }

    @Test
    void lastsTenMinutes() {
        StageTestAttempt attempt = attempt(20);

        assertThat(attempt.getExpiresAt()).isEqualTo(START.plus(Duration.ofMinutes(10)));
        assertThat(attempt.isInProgress(START.plusSeconds(599))).isTrue();
        assertThat(attempt.isInProgress(START.plusSeconds(600))).isFalse();
    }

    @Test
    void eighteenCorrectAnswersPass() {
        StageTestAttempt attempt = attempt(20);

        attempt.submit(answers(18), START.plusSeconds(60));

        assertThat(attempt.getCorrectCount()).isEqualTo(18);
        assertThat(attempt.isPassed()).isTrue();
        assertThat(attempt.isFailed(START.plusSeconds(60))).isFalse();
    }

    @Test
    void seventeenCorrectAnswersFailAndStartTheCooldown() {
        StageTestAttempt attempt = attempt(20);
        Instant submittedAt = START.plusSeconds(60);

        attempt.submit(answers(17), submittedAt);

        assertThat(attempt.isPassed()).isFalse();
        assertThat(attempt.isFailed(submittedAt)).isTrue();
        assertThat(attempt.retryAt()).isEqualTo(submittedAt.plus(Duration.ofHours(24)));
    }

    @Test
    void answersAreTrimmedAndCaseInsensitiveAndBlankIsWrong() {
        StageTestAttempt attempt = attempt(20);
        Map<Integer, String> answers = answers(20);
        answers.put(1, "  WORD1 ");
        answers.put(2, "   ");
        answers.remove(3);

        attempt.submit(answers, START.plusSeconds(60));

        List<StageTestQuestion> questions = attempt.getQuestions();
        assertThat(questions.get(0).isCorrect()).isTrue();
        assertThat(questions.get(0).getAnswer()).isEqualTo("WORD1");
        assertThat(questions.get(1).isCorrect()).isFalse();
        assertThat(questions.get(1).getAnswer()).isNull();
        assertThat(questions.get(2).isCorrect()).isFalse();
        assertThat(attempt.getCorrectCount()).isEqualTo(18);
    }

    @Test
    void submissionWithinTheGraceIsStillGraded() {
        StageTestAttempt attempt = attempt(20);

        attempt.submit(answers(20), attempt.getExpiresAt().plusSeconds(30));

        assertThat(attempt.getCorrectCount()).isEqualTo(20);
    }

    @Test
    void lateSubmissionCountsEveryQuestionAsWrong() {
        StageTestAttempt attempt = attempt(20);

        attempt.submit(answers(20), attempt.getExpiresAt().plusSeconds(31));

        assertThat(attempt.getCorrectCount()).isZero();
        assertThat(attempt.isPassed()).isFalse();
    }

    @Test
    void attemptNeverSubmittedFailsWhenTimeIsUp() {
        StageTestAttempt attempt = attempt(20);

        assertThat(attempt.isFailed(attempt.getExpiresAt().minusSeconds(1))).isFalse();
        assertThat(attempt.isFailed(attempt.getExpiresAt())).isTrue();
        assertThat(attempt.retryAt()).isEqualTo(attempt.getExpiresAt().plus(Duration.ofHours(24)));
    }

    @Test
    void cannotBeSubmittedTwice() {
        StageTestAttempt attempt = attempt(20);
        attempt.submit(answers(20), START.plusSeconds(60));

        assertThatIllegalStateException().isThrownBy(() -> attempt.submit(answers(20), START.plusSeconds(61)));
    }
}
