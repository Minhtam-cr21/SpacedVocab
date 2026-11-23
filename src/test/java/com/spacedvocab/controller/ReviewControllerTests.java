package com.spacedvocab.controller;

import com.spacedvocab.IntegrationTest;
import com.spacedvocab.algorithm.SchedulingState;
import com.spacedvocab.algorithm.SpacedRepetitionAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Review flow (D7, D13) with a stub algorithm, so these tests check scheduling plumbing and R5–R8,
 * not the SM-2 formula (Sm2AlgorithmTest does that).
 */
@IntegrationTest
class ReviewControllerTests extends ApiTestSupport {

    /** Not SM-2: quality ≥ 3 adds a repetition, otherwise resets it; interval = max(1, quality) days. */
    @TestConfiguration
    static class StubAlgorithmConfig {
        @Bean
        @Primary
        SpacedRepetitionAlgorithm stubAlgorithm() {
            return (state, quality) -> new SchedulingState(
                    state.easinessFactor(),
                    quality.value() >= 3 ? state.repetitions() + 1 : 0,
                    Math.max(1, quality.value()));
        }
    }

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    JdbcTemplate jdbc;

    private MvcTestResult due(Session session, String query) {
        return getWithToken("/api/reviews/due" + query, session.accessToken());
    }

    private MvcTestResult review(Session session, long cardId, int quality) {
        return postJson("/api/reviews", session.accessToken(), """
                {"cardId": %d, "quality": %d}
                """.formatted(cardId, quality));
    }

    private void setDailyNewLimit(Session session, int limit) {
        assertThat(putJson("/api/me/settings", session.accessToken(), """
                {"dailyNewLimit": %d, "timezone": "%s"}
                """.formatted(limit, ZONE.getId()))).hasStatus2xxSuccessful();
    }

    /** Moves the card's next review into the past, as if its day had come. */
    private void makeDue(long cardId) {
        jdbc.update("update card_progress set next_review_at = now() - interval '1 day' where card_id = ?", cardId);
    }

    @Test
    void listsNewCardsWithPreviewIntervalsForTheFourButtons() {
        Session session = registerNew();
        long deckId = createDeck(session.accessToken(), "Verbs", "PRIVATE");
        createCard(session.accessToken(), deckId, "abandon", "từ bỏ");
        createCard(session.accessToken(), deckId, "abide", "tuân theo");

        MvcTestResult result = due(session, "");

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.reviewCount").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.newCount").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.nextDueAt").isNull();
        assertThat(result).bodyJson().extractingPath("$.cards.length()").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.cards[0].word").isEqualTo("abandon");
        assertThat(result).bodyJson().extractingPath("$.cards[0].deckName").isEqualTo("Verbs");
        assertThat(result).bodyJson().extractingPath("$.cards[0].state").isEqualTo("NEW");
        assertThat(result).bodyJson().extractingPath("$.cards[0].previewIntervals['1']").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.cards[0].previewIntervals['3']").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.cards[0].previewIntervals['4']").isEqualTo(4);
        assertThat(result).bodyJson().extractingPath("$.cards[0].previewIntervals['5']").isEqualTo(5);
    }

    @Test
    void limitZeroReturnsOnlyTheCounts() {
        Session session = registerNew();
        createCard(session.accessToken(), createDeck(session.accessToken(), "D", "PRIVATE"), "cat", "mèo");

        MvcTestResult result = due(session, "?limit=0");

        assertThat(result).bodyJson().extractingPath("$.newCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.cards.length()").isEqualTo(0);
        assertThat(due(session, "?limit=201")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void gradingSchedulesTheCardInTheLearnerTimezone() {
        Session session = registerNew();
        long deckId = createDeck(session.accessToken(), "D", "PRIVATE");
        long cardId = createCard(session.accessToken(), deckId, "cat", "mèo");
        createCard(session.accessToken(), deckId, "dog", "chó");

        MvcTestResult result = review(session, cardId, 4);

        LocalDate expectedDate = LocalDate.now(ZONE).plusDays(4);
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.cardId").isEqualTo((int) cardId);
        assertThat(result).bodyJson().extractingPath("$.state").isEqualTo("LEARNING");
        assertThat(result).bodyJson().extractingPath("$.repetitions").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.intervalDays").isEqualTo(4);
        assertThat(result).bodyJson().extractingPath("$.easinessFactor").isEqualTo(2.5);
        assertThat(result).bodyJson().extractingPath("$.nextReviewDate").isEqualTo(expectedDate.toString());
        assertThat(result).bodyJson().extractingPath("$.nextReviewAt")
                .isEqualTo(expectedDate.atStartOfDay(ZONE).toInstant().toString());

        MvcTestResult after = due(session, "");
        assertThat(after).bodyJson().extractingPath("$.reviewCount").isEqualTo(0);
        assertThat(after).bodyJson().extractingPath("$.newCount").isEqualTo(1);
        assertThat(after).bodyJson().extractingPath("$.cards[0].word").isEqualTo("dog");
    }

    @Test
    void cardComesBackWhenItsDayComesAndIsListedBeforeNewCards() {
        Session session = registerNew();
        long deckId = createDeck(session.accessToken(), "D", "PRIVATE");
        long cardId = createCard(session.accessToken(), deckId, "cat", "mèo");
        createCard(session.accessToken(), deckId, "dog", "chó");
        review(session, cardId, 4);
        makeDue(cardId);

        MvcTestResult result = due(session, "");

        assertThat(result).bodyJson().extractingPath("$.reviewCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.newCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.cards[0].word").isEqualTo("cat");
        assertThat(result).bodyJson().extractingPath("$.cards[0].state").isEqualTo("LEARNING");
        assertThat(result).bodyJson().extractingPath("$.cards[1].state").isEqualTo("NEW");
    }

    @Test
    void reachesReviewStateAndCountsALapseWhenForgotten() {
        Session session = registerNew();
        long cardId = createCard(session.accessToken(), createDeck(session.accessToken(), "D", "PRIVATE"), "cat", "mèo");

        review(session, cardId, 4);
        assertThat(review(session, cardId, 5)).bodyJson().extractingPath("$.state").isEqualTo("REVIEW");

        MvcTestResult forgotten = review(session, cardId, 1);
        assertThat(forgotten).bodyJson().extractingPath("$.state").isEqualTo("LEARNING");
        assertThat(forgotten).bodyJson().extractingPath("$.repetitions").isEqualTo(0);
        assertThat(forgotten).bodyJson().extractingPath("$.intervalDays").isEqualTo(1);
        assertThat(forgotten).bodyJson().extractingPath("$.lapses").isEqualTo(1);
    }

    @Test
    void newCardsPerDayAreCappedByTheDailyLimit_R7() {
        Session session = registerNew();
        setDailyNewLimit(session, 1);
        long deckId = createDeck(session.accessToken(), "D", "PRIVATE");
        long first = createCard(session.accessToken(), deckId, "cat", "mèo");
        long second = createCard(session.accessToken(), deckId, "dog", "chó");

        MvcTestResult before = due(session, "");
        assertThat(before).bodyJson().extractingPath("$.newCount").isEqualTo(1);
        assertThat(before).bodyJson().extractingPath("$.cards.length()").isEqualTo(1);

        assertThat(review(session, first, 1)).hasStatusOk();

        MvcTestResult after = due(session, "");
        assertThat(after).bodyJson().extractingPath("$.newCount").isEqualTo(0);
        assertThat(after).bodyJson().extractingPath("$.nextDueAt")
                .isEqualTo(LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).toInstant().toString());

        MvcTestResult blocked = review(session, second, 4);
        assertThat(blocked).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(blocked).bodyJson().extractingPath("$.code").isEqualTo("DAILY_NEW_LIMIT_REACHED");

        // re-grading a card already started today (D13 puts "Quên" back in the queue) is still allowed
        assertThat(review(session, first, 4)).hasStatusOk();
    }

    @Test
    void cannotGradeCardsOutsideTheLibrary_R6() {
        Session owner = registerNew();
        long publicCard = createCard(owner.accessToken(), createDeck(owner.accessToken(), "Pub", "PUBLIC"), "cat", "mèo");
        long privateCard = createCard(owner.accessToken(), createDeck(owner.accessToken(), "Priv", "PRIVATE"), "dog", "chó");
        Session stranger = registerNew();

        MvcTestResult notInLibrary = review(stranger, publicCard, 4);
        assertThat(notInLibrary).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(notInLibrary).bodyJson().extractingPath("$.code").isEqualTo("CARD_NOT_IN_LIBRARY");

        assertThat(review(stranger, privateCard, 4)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(review(stranger, Long.MAX_VALUE, 4)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletedCardsAndDecksAreNeitherDueNorGradable_R8() {
        Session session = registerNew();
        long deckId = createDeck(session.accessToken(), "D", "PRIVATE");
        long cardId = createCard(session.accessToken(), deckId, "cat", "mèo");
        long otherCard = createCard(session.accessToken(), deckId, "dog", "chó");
        review(session, cardId, 4);
        makeDue(cardId);

        deleteWithToken("/api/decks/%d/cards/%d".formatted(deckId, otherCard), session.accessToken());
        assertThat(due(session, "")).bodyJson().extractingPath("$.newCount").isEqualTo(0);
        assertThat(review(session, otherCard, 4)).hasStatus(HttpStatus.NOT_FOUND);

        deleteWithToken("/api/decks/" + deckId, session.accessToken());
        assertThat(due(session, "")).bodyJson().extractingPath("$.reviewCount").isEqualTo(0);
    }

    @Test
    void rejectsQualityOutsideZeroToFive_R5() {
        Session session = registerNew();
        long cardId = createCard(session.accessToken(), createDeck(session.accessToken(), "D", "PRIVATE"), "cat", "mèo");

        MvcTestResult result = review(session, cardId, 6);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("quality");
        assertThat(postJson("/api/reviews", session.accessToken(), "{\"quality\": 4}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void canBeLimitedToOneDeck() {
        Session session = registerNew();
        long deckA = createDeck(session.accessToken(), "A", "PRIVATE");
        long deckB = createDeck(session.accessToken(), "B", "PRIVATE");
        createCard(session.accessToken(), deckA, "cat", "mèo");
        createCard(session.accessToken(), deckB, "dog", "chó");

        MvcTestResult result = due(session, "?deckId=" + deckB);

        assertThat(result).bodyJson().extractingPath("$.newCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.cards[0].word").isEqualTo("dog");

        long strangersDeck = createDeck(registerNew().accessToken(), "Secret", "PRIVATE");
        assertThat(due(session, "?deckId=" + strangersDeck)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void writesAnAppendOnlyReviewLog_R8() {
        Session session = registerNew();
        long cardId = createCard(session.accessToken(), createDeck(session.accessToken(), "D", "PRIVATE"), "cat", "mèo");

        review(session, cardId, 4);
        review(session, cardId, 1);

        assertThat(jdbc.queryForList(
                "select state_before, quality, interval_after from review_logs where card_id = ? order by id", cardId))
                .extracting(row -> row.get("state_before") + "/" + row.get("quality") + "/" + row.get("interval_after"))
                .containsExactly("NEW/4/4", "LEARNING/1/1");
        assertThatExceptionOfType(DataAccessException.class)
                .isThrownBy(() -> jdbc.update("update review_logs set quality = 0 where card_id = ?", cardId));
        assertThatExceptionOfType(DataAccessException.class)
                .isThrownBy(() -> jdbc.update("delete from review_logs where card_id = ?", cardId));
    }

    @Test
    void deckDetailShowsTheViewersProgressByState() {
        Session session = registerNew();
        long deckId = createDeck(session.accessToken(), "D", "PRIVATE");
        long mastered = createCard(session.accessToken(), deckId, "cat", "mèo");
        long started = createCard(session.accessToken(), deckId, "dog", "chó");
        createCard(session.accessToken(), deckId, "fox", "cáo");
        review(session, mastered, 4);
        review(session, mastered, 4);
        review(session, started, 3);

        MvcTestResult detail = getWithToken("/api/decks/" + deckId, session.accessToken());

        assertThat(detail).bodyJson().extractingPath("$.progress.newCount").isEqualTo(1);
        assertThat(detail).bodyJson().extractingPath("$.progress.learningCount").isEqualTo(1);
        assertThat(detail).bodyJson().extractingPath("$.progress.reviewCount").isEqualTo(1);
        assertThat(getWithToken("/api/decks?scope=owned", session.accessToken())).bodyJson()
                .extractingPath("$[0].progress").isNull();
    }
}
