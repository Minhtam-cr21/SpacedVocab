package com.spacedvocab.entity;

import com.spacedvocab.algorithm.EasinessFactor;
import com.spacedvocab.algorithm.Quality;
import com.spacedvocab.algorithm.SchedulingState;
import com.spacedvocab.algorithm.SpacedRepetitionAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** Uses stub algorithms: the real SM-2 has its own test (written in M3 tutor mode). */
class CardProgressTest {

    private static final ZoneId HCM = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 20:00 UTC on 23/11 is already 03:00 on 24/11 in Ho Chi Minh City. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T20:00:00Z"), ZoneOffset.UTC);

    private static CardProgress newProgress() {
        User user = User.newLearner("learner@example.com", "{noop}x", "Learner", HCM);
        ReflectionTestUtils.setField(user, "id", 1L);
        Card card = new Card(new Deck(user, "Deck", null, DeckVisibility.PRIVATE),
                new CardContent("abandon", "từ bỏ", null, null, null, null));
        ReflectionTestUtils.setField(card, "id", 10L);
        return new CardProgress(user, card);
    }

    /** Always returns the given state. */
    private static SpacedRepetitionAlgorithm returning(int repetitions, int intervalDays, double ef) {
        return (current, quality) -> new SchedulingState(EasinessFactor.of(ef), repetitions, intervalDays);
    }

    @Test
    void startsAsNewCardWithInitialState() {
        CardProgress progress = newProgress();

        assertThat(progress.getState()).isEqualTo(CardState.NEW);
        assertThat(progress.schedulingState()).isEqualTo(SchedulingState.INITIAL);
        assertThat(progress.getNextReviewAt()).isNull();
        assertThat(progress.nextReviewDate()).isNull();
    }

    @Test
    void storesAlgorithmResultAndSchedulesStartOfDayInLearnerTimezone_R7() {
        CardProgress progress = newProgress();

        progress.applyReview(Quality.of(4), returning(1, 3, 2.4), CLOCK);

        assertThat(progress.schedulingState()).isEqualTo(new SchedulingState(EasinessFactor.of(2.4), 1, 3));
        assertThat(progress.getState()).isEqualTo(CardState.LEARNING);
        assertThat(progress.nextReviewDate()).isEqualTo(LocalDate.of(2026, 11, 27)); // 24/11 + 3 days
        assertThat(progress.getNextReviewAt()).isEqualTo(Instant.parse("2026-11-26T17:00:00Z")); // 00:00 +07:00
        assertThat(progress.getLastReviewedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void becomesReviewAfterTwoRepetitions() {
        CardProgress progress = newProgress();

        progress.applyReview(Quality.of(5), returning(2, 6, 2.5), CLOCK);

        assertThat(progress.getState()).isEqualTo(CardState.REVIEW);
    }

    @Test
    void countsLapseOnlyWhenARememberedCardIsForgotten() {
        CardProgress progress = newProgress();

        progress.applyReview(Quality.of(1), returning(0, 1, 2.5), CLOCK);
        assertThat(progress.getLapses()).isZero(); // a new card failed: not a lapse

        progress.applyReview(Quality.of(4), returning(1, 1, 2.5), CLOCK);
        progress.applyReview(Quality.of(1), returning(0, 1, 2.5), CLOCK);
        assertThat(progress.getLapses()).isEqualTo(1);
        assertThat(progress.getState()).isEqualTo(CardState.LEARNING);
    }

    @Test
    void producesLogWithValuesBeforeAndAfter_R8() {
        CardProgress progress = newProgress();

        ReviewLog log = progress.applyReview(Quality.of(3), returning(1, 1, 2.36), CLOCK);

        assertThat(log.getUserId()).isEqualTo(1L);
        assertThat(log.getCardId()).isEqualTo(10L);
        assertThat(log.getQuality()).isEqualTo(3);
        assertThat(log.getStateBefore()).isEqualTo(CardState.NEW);
        assertThat(log.getStateAfter()).isEqualTo(CardState.LEARNING);
        assertThat(log.getEfBefore()).isEqualTo(2.5);
        assertThat(log.getEfAfter()).isEqualTo(2.36);
        assertThat(log.getIntervalBefore()).isZero();
        assertThat(log.getIntervalAfter()).isEqualTo(1);
        assertThat(log.getReviewedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void rejectsAlgorithmThatSchedulesLessThanOneDay_R5() {
        CardProgress progress = newProgress();

        assertThatIllegalStateException()
                .isThrownBy(() -> progress.applyReview(Quality.of(4), returning(1, 0, 2.5), CLOCK));
        assertThat(progress.getState()).isEqualTo(CardState.NEW);
        assertThat(progress.schedulingState()).isEqualTo(SchedulingState.INITIAL);
    }
}
