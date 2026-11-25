package com.spacedvocab.controller;

import com.jayway.jsonpath.JsonPath;
import com.spacedvocab.IntegrationTest;
import com.spacedvocab.algorithm.SchedulingState;
import com.spacedvocab.algorithm.SpacedRepetitionAlgorithm;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CEFR learning path (business-spec §6, R10–R12). Every test builds its own path with three stages
 * so lessons added by other tests never change its progress. Reviews use a stub algorithm.
 */
@IntegrationTest
class PathControllerTests extends ApiTestSupport {

    @TestConfiguration
    static class StubAlgorithmConfig {
        @Bean
        @Primary
        SpacedRepetitionAlgorithm stubAlgorithm() {
            return (state, quality) -> new SchedulingState(state.easinessFactor(), state.repetitions() + 1, 1);
        }
    }

    private static final String MEANING_PREFIX = "nghĩa của ";

    @Autowired
    JdbcTemplate jdbc;

    private String adminToken;
    private long pathId;
    private List<Long> stageIds;

    @BeforeEach
    void createPath() {
        adminToken = toSession("admin@test.local", login("admin@test.local", "admin-test-password")).accessToken();
        pathId = jdbc.queryForObject("""
                insert into learning_paths (name, created_at, updated_at) values (?, now(), now()) returning id
                """, Long.class, "Path " + UUID.randomUUID());
        stageIds = new ArrayList<>();
        String[][] stages = {{"A1", "Căn bản"}, {"A2", "Sơ cấp"}, {"B1", "Trung cấp"}};
        for (int i = 0; i < stages.length; i++) {
            stageIds.add(jdbc.queryForObject("""
                    insert into path_stages (path_id, cefr_level, title, position, created_at, updated_at)
                    values (?, ?, ?, ?, now(), now()) returning id
                    """, Long.class, pathId, stages[i][0], stages[i][1], i + 1));
        }
    }

    // ---------- helpers ----------

    /** An OFFICIAL deck owned by the admin with {@code cards} cards; returns [deckId, cardIds...]. */
    private List<Long> officialDeck(int cards) {
        long adminId = jdbc.queryForObject("select id from users where email = 'admin@test.local'", Long.class);
        long deckId = jdbc.queryForObject("""
                insert into decks (owner_id, name, visibility, created_at, updated_at)
                values (?, ?, 'OFFICIAL', now(), now()) returning id
                """, Long.class, adminId, "Official " + UUID.randomUUID());
        List<Long> ids = new ArrayList<>(List.of(deckId));
        for (int i = 0; i < cards; i++) {
            String word = "w" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
            ids.add(jdbc.queryForObject("""
                    insert into cards (deck_id, word, meaning, part_of_speech, created_at, updated_at)
                    values (?, ?, ?, 'noun', now(), now()) returning id
                    """, Long.class, deckId, word, MEANING_PREFIX + word));
        }
        return ids;
    }

    /** Adds the deck as a lesson through the admin API and returns the lesson id. */
    private long addLesson(int stageIndex, long deckId, int position) {
        MvcTestResult result = postJson("/api/admin/stages/%d/lessons".formatted(stageIds.get(stageIndex)), adminToken, """
                {"deckId": %d, "position": %d}
                """.formatted(deckId, position));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ((Number) JsonPath.read(body(result), "$.id")).longValue();
    }

    /** A lesson with {@code cards} cards; returns [lessonId, cardIds...]. */
    private List<Long> lesson(int stageIndex, int cards) {
        List<Long> deck = officialDeck(cards);
        List<Long> ids = new ArrayList<>(List.of(addLesson(stageIndex, deck.get(0), 1)));
        ids.addAll(deck.subList(1, deck.size()));
        return ids;
    }

    private MvcTestResult path(Session session) {
        return getWithToken("/api/paths/" + pathId, session.accessToken());
    }

    private MvcTestResult startLesson(Session session, long lessonId) {
        return postJson("/api/lessons/%d/start".formatted(lessonId), session.accessToken(), "{}");
    }

    private MvcTestResult startTest(Session session, int stageIndex) {
        return postJson("/api/stages/%d/test-attempts".formatted(stageIds.get(stageIndex)), session.accessToken(), "{}");
    }

    private void review(Session session, long cardId) {
        assertThat(postJson("/api/reviews", session.accessToken(), """
                {"cardId": %d, "quality": 4}
                """.formatted(cardId))).hasStatusOk();
    }

    /** Answers built from the meanings; {@code correct} of them are right, the rest wrong. */
    private String answers(MvcTestResult started, int correct) {
        List<Map<String, Object>> questions = JsonPath.read(body(started), "$.questions");
        StringBuilder json = new StringBuilder("{\"answers\": [");
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            String word = ((String) q.get("meaning")).substring(MEANING_PREFIX.length());
            String answer = i < correct ? "  " + word.toUpperCase() + " " : "wrong";
            json.append(i == 0 ? "" : ",")
                    .append("{\"position\": %s, \"answer\": \"%s\"}".formatted(q.get("position"), answer));
        }
        return json.append("]}").toString();
    }

    private MvcTestResult submit(Session session, MvcTestResult started, String answersJson) {
        long attemptId = ((Number) JsonPath.read(body(started), "$.attemptId")).longValue();
        return postJson("/api/stage-test-attempts/%d/submit".formatted(attemptId), session.accessToken(), answersJson);
    }

    // ---------- seeded content ----------

    @Test
    void cefrPathComesWithSixStagesAndTheA1Lessons() {
        Session learner = registerNew();
        List<Map<String, Object>> paths = JsonPath.read(body(getWithToken("/api/paths", learner.accessToken())), "$");
        Object cefrId = paths.stream().filter(p -> "Lộ trình CEFR".equals(p.get("name"))).findFirst().orElseThrow().get("id");

        MvcTestResult result = getWithToken("/api/paths/" + cefrId, learner.accessToken());

        assertThat(result).bodyJson().extractingPath("$.stages.length()").isEqualTo(6);
        assertThat(result).bodyJson().extractingPath("$.stages[0].cefrLevel").isEqualTo("A1");
        assertThat(result).bodyJson().extractingPath("$.stages[0].lessonCount").isEqualTo(7);
        assertThat(result).bodyJson().extractingPath("$.stages[0].testAvailable").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.stages[5].cefrLevel").isEqualTo("C2");
    }

    // ---------- R11: opening stages ----------

    @Test
    void onlyTheFirstStageIsOpenAtTheStart() {
        lesson(0, 2);
        Session learner = registerNew();

        MvcTestResult result = path(learner);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.stages.length()").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.stages[0].cefrLevel").isEqualTo("A1");
        assertThat(result).bodyJson().extractingPath("$.stages[0].unlocked").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.stages[0].lessonCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.stages[0].completedLessonCount").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.stages[1].unlocked").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.stages[2].unlocked").isEqualTo(false);
    }

    @Test
    void lessonOfALockedStageCannotBeStarted() {
        long lessonId = lesson(1, 2).get(0);
        Session learner = registerNew();

        MvcTestResult result = startLesson(learner, lessonId);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("STAGE_LOCKED");
    }

    @Test
    void startingALessonPutsItsCardsInTheReviewQueue() {
        List<Long> lesson = lesson(0, 2);
        Session learner = registerNew();

        MvcTestResult started = startLesson(learner, lesson.get(0));

        assertThat(started).hasStatusOk();
        long deckId = ((Number) JsonPath.read(body(started), "$.deckId")).longValue();
        MvcTestResult due = getWithToken("/api/reviews/due?deckId=" + deckId, learner.accessToken());
        assertThat(due).bodyJson().extractingPath("$.newCount").isEqualTo(2);
        review(learner, lesson.get(1)); // R6: a subscribed deck is in the library
    }

    @Test
    void completingEveryLessonOpensTheNextStage() {
        List<Long> first = lesson(0, 2);
        List<Long> second = lesson(0, 1);
        Session learner = registerNew();
        startLesson(learner, first.get(0));
        startLesson(learner, second.get(0));
        review(learner, first.get(1));
        review(learner, first.get(2));

        MvcTestResult halfway = path(learner);
        assertThat(halfway).bodyJson().extractingPath("$.stages[0].completedLessonCount").isEqualTo(1);
        assertThat(halfway).bodyJson().extractingPath("$.stages[1].unlocked").isEqualTo(false);

        review(learner, second.get(1));
        MvcTestResult done = path(learner);

        assertThat(done).bodyJson().extractingPath("$.stages[0].completedLessonCount").isEqualTo(2);
        assertThat(done).bodyJson().extractingPath("$.stages[1].unlocked").isEqualTo(true);
        assertThat(done).bodyJson().extractingPath("$.stages[1].unlockMethod").isEqualTo("LESSONS");
        assertThat(done).bodyJson().extractingPath("$.stages[2].unlocked").isEqualTo(false);
    }

    @Test
    void anOpenedStageStaysOpenWhenALessonIsAddedBefore() {
        List<Long> first = lesson(0, 1);
        Session learner = registerNew();
        startLesson(learner, first.get(0));
        review(learner, first.get(1));
        assertThat(path(learner)).bodyJson().extractingPath("$.stages[1].unlocked").isEqualTo(true);

        lesson(0, 3);

        MvcTestResult result = path(learner);
        assertThat(result).bodyJson().extractingPath("$.stages[0].completedLessonCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.stages[1].unlocked").isEqualTo(true);
    }

    @Test
    void stageShowsItsLessonsWithProgress() {
        List<Long> lesson = lesson(0, 2);
        Session learner = registerNew();
        startLesson(learner, lesson.get(0));
        review(learner, lesson.get(1));

        MvcTestResult result = getWithToken("/api/stages/" + stageIds.get(0), learner.accessToken());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.stage.cefrLevel").isEqualTo("A1");
        assertThat(result).bodyJson().extractingPath("$.lessons[0].cardCount").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.lessons[0].learnedCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.lessons[0].completed").isEqualTo(false);
    }

    // ---------- R12: stage test ----------

    @Test
    void passingTheStageTestOpensTheNextStage() {
        lesson(0, 12);
        lesson(0, 12);
        Session learner = registerNew();
        assertThat(path(learner)).bodyJson().extractingPath("$.stages[0].testAvailable").isEqualTo(true);

        MvcTestResult started = startTest(learner, 0);
        assertThat(started).hasStatus(HttpStatus.CREATED);
        assertThat(started).bodyJson().extractingPath("$.questions.length()").isEqualTo(20);
        assertThat(body(started)).doesNotContain("\"word\"").doesNotContain("correctWord");

        MvcTestResult result = submit(learner, started, answers(started, 18));

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.correctCount").isEqualTo(18);
        assertThat(result).bodyJson().extractingPath("$.passed").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.nextStageUnlocked").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.questions[0].correctWord").isNotNull();
        MvcTestResult after = path(learner);
        assertThat(after).bodyJson().extractingPath("$.stages[1].unlocked").isEqualTo(true);
        assertThat(after).bodyJson().extractingPath("$.stages[1].unlockMethod").isEqualTo("TEST");
        assertThat(after).bodyJson().extractingPath("$.stages[0].completedLessonCount").isEqualTo(0);
        assertThat(after).bodyJson().extractingPath("$.stages[0].testAvailable").isEqualTo(false);
    }

    @Test
    void startingAgainReturnsTheAttemptInProgress() {
        lesson(0, 20);
        Session learner = registerNew();
        MvcTestResult first = startTest(learner, 0);

        MvcTestResult again = startTest(learner, 0);

        assertThat(again).hasStatusOk();
        assertThat(JsonPath.<Integer>read(body(again), "$.attemptId"))
                .isEqualTo(JsonPath.<Integer>read(body(first), "$.attemptId"));
    }

    @Test
    void failingTheTestMeansWaiting24Hours() {
        lesson(0, 20);
        Session learner = registerNew();
        MvcTestResult started = startTest(learner, 0);

        MvcTestResult result = submit(learner, started, answers(started, 17));

        assertThat(result).bodyJson().extractingPath("$.passed").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.nextStageUnlocked").isEqualTo(false);
        MvcTestResult retry = startTest(learner, 0);
        assertThat(retry).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(retry).bodyJson().extractingPath("$.code").isEqualTo("STAGE_TEST_COOLDOWN");
        MvcTestResult view = path(learner);
        assertThat(view).bodyJson().extractingPath("$.stages[0].testAvailable").isEqualTo(false);
        assertThat(view).bodyJson().extractingPath("$.stages[0].testRetryAt").isNotNull();
        assertThat(view).bodyJson().extractingPath("$.stages[1].unlocked").isEqualTo(false);

        long attemptId = ((Number) JsonPath.read(body(started), "$.attemptId")).longValue();
        jdbc.update("update stage_test_attempts set submitted_at = now() - interval '25 hours' where id = ?", attemptId);
        assertThat(startTest(learner, 0)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void lateAnswersAreNotCounted() {
        lesson(0, 20);
        Session learner = registerNew();
        MvcTestResult started = startTest(learner, 0);
        long attemptId = ((Number) JsonPath.read(body(started), "$.attemptId")).longValue();
        jdbc.update("update stage_test_attempts set expires_at = now() - interval '1 minute' where id = ?", attemptId);

        MvcTestResult result = submit(learner, started, answers(started, 20));

        assertThat(result).bodyJson().extractingPath("$.correctCount").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.passed").isEqualTo(false);
    }

    @Test
    void anAttemptIsSubmittedOnlyOnce() {
        lesson(0, 20);
        Session learner = registerNew();
        MvcTestResult started = startTest(learner, 0);
        submit(learner, started, answers(started, 20));

        MvcTestResult again = submit(learner, started, answers(started, 20));

        assertThat(again).hasStatus(HttpStatus.CONFLICT);
        assertThat(again).bodyJson().extractingPath("$.code").isEqualTo("STAGE_TEST_ALREADY_SUBMITTED");
    }

    @Test
    void someoneElsesAttemptIsNotFound() {
        lesson(0, 20);
        Session owner = registerNew();
        MvcTestResult started = startTest(owner, 0);

        assertThat(submit(registerNew(), started, answers(started, 20))).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void stageWithFewerThan20WordsHasNoTest() {
        lesson(0, 19);
        Session learner = registerNew();
        assertThat(path(learner)).bodyJson().extractingPath("$.stages[0].testAvailable").isEqualTo(false);

        MvcTestResult result = startTest(learner, 0);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("STAGE_TEST_TOO_FEW_CARDS");
    }

    @Test
    void testOfALockedStageIsRefused() {
        lesson(1, 20);
        Session learner = registerNew();

        MvcTestResult result = startTest(learner, 1);

        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("STAGE_LOCKED");
    }

    @Test
    void testIsNotAvailableOnceTheNextStageIsOpen() {
        List<Long> first = lesson(0, 1);
        Session learner = registerNew();
        startLesson(learner, first.get(0));
        review(learner, first.get(1));

        MvcTestResult result = startTest(learner, 0);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("STAGE_TEST_NOT_AVAILABLE");
    }

    @Test
    void invalidAnswerPositionIsAValidationError() {
        lesson(0, 20);
        Session learner = registerNew();
        MvcTestResult started = startTest(learner, 0);

        MvcTestResult result = submit(learner, started, """
                {"answers": [{"position": 21, "answer": "x"}]}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
    }

    // ---------- R10: admin ----------

    @Test
    void onlyOfficialDecksCanBeLessons() {
        Session learner = registerNew();
        long publicDeck = createDeck(learner.accessToken(), "Public " + UUID.randomUUID(), "PUBLIC");

        MvcTestResult result = postJson("/api/admin/stages/%d/lessons".formatted(stageIds.get(0)), adminToken, """
                {"deckId": %d, "position": 1}
                """.formatted(publicDeck));

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("LESSON_DECK_NOT_OFFICIAL");
    }

    @Test
    void aDeckIsAtMostOneLesson() {
        long deckId = officialDeck(1).get(0);
        addLesson(0, deckId, 1);

        MvcTestResult result = postJson("/api/admin/stages/%d/lessons".formatted(stageIds.get(1)), adminToken, """
                {"deckId": %d, "position": 1}
                """.formatted(deckId));

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("DECK_ALREADY_IN_PATH");
    }

    @Test
    void learnersCannotManageThePath() {
        long deckId = officialDeck(1).get(0);
        Session learner = registerNew();

        MvcTestResult result = postJson("/api/admin/stages/%d/lessons".formatted(stageIds.get(0)),
                learner.accessToken(), """
                {"deckId": %d, "position": 1}
                """.formatted(deckId));

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminRenamesStagesAndReordersLessons() {
        long lessonId = lesson(0, 1).get(0);

        assertThat(putJson("/api/admin/stages/" + stageIds.get(0), adminToken, """
                {"title": "Khởi động"}
                """)).hasStatus(HttpStatus.NO_CONTENT);
        MvcTestResult moved = putJson("/api/admin/lessons/" + lessonId, adminToken, """
                {"position": 3}
                """);

        assertThat(moved).bodyJson().extractingPath("$.position").isEqualTo(3);
        assertThat(path(registerNew())).bodyJson().extractingPath("$.stages[0].title").isEqualTo("Khởi động");
        assertThat(deleteWithToken("/api/admin/lessons/" + lessonId, adminToken)).hasStatus(HttpStatus.NO_CONTENT);
    }
}
