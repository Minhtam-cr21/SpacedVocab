package com.spacedvocab.controller;

import com.spacedvocab.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CardControllerTests extends ApiTestSupport {

    private static String cards(long deckId) {
        return "/api/decks/%d/cards".formatted(deckId);
    }

    @Nested
    class Create {

        @Test
        void addsCardWithAllFields() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Verbs", "PRIVATE");

            MvcTestResult result = postJson(cards(deckId), session.accessToken(), """
                    {"word": " abandon ", "meaning": "từ bỏ", "ipa": "/əˈbændən/", "partOfSpeech": "verb",
                     "example": "They abandoned the car.", "note": ""}
                    """);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson().extractingPath("$.word").isEqualTo("abandon");
            assertThat(result).bodyJson().extractingPath("$.ipa").isEqualTo("/əˈbændən/");
            assertThat(result).bodyJson().extractingPath("$.note").isNull();
            assertThat(result).bodyJson().extractingPath("$.deckId").isEqualTo((int) deckId);
            assertThat(getWithToken("/api/decks/" + deckId, session.accessToken())).bodyJson()
                    .extractingPath("$.cardCount").isEqualTo(1);
        }

        @Test
        void requiresWordAndMeaning() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Empty", "PRIVATE");

            MvcTestResult result = postJson(cards(deckId), session.accessToken(), """
                    {"word": "", "meaning": "  "}
                    """);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.errors.length()").isEqualTo(2);
        }

        @Test
        void rejectsDuplicateWordInTheSameDeckIgnoringCase_R2() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Animals", "PRIVATE");
            createCard(session.accessToken(), deckId, "Cat", "con mèo");

            MvcTestResult result = postJson(cards(deckId), session.accessToken(), """
                    {"word": "cat", "meaning": "mèo"}
                    """);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("CARD_WORD_TAKEN");
            assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("word");
            assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("\"cat\" đã có trong deck này.");
        }

        @Test
        void allowsTheSameWordInAnotherDeck_R2() {
            Session session = registerNew();
            createCard(session.accessToken(), createDeck(session.accessToken(), "A", "PRIVATE"), "run", "chạy");

            createCard(session.accessToken(), createDeck(session.accessToken(), "B", "PRIVATE"), "run", "chạy");
        }

        @Test
        void otherUserCannotAddCardsToPublicDeck_R3() {
            long deckId = createDeck(registerNew().accessToken(), "Public", "PUBLIC");
            Session stranger = registerNew();

            MvcTestResult result = postJson(cards(deckId), stranger.accessToken(), """
                    {"word": "spam", "meaning": "rác"}
                    """);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void otherUserAddingCardsToPrivateDeckGetsNotFound_R3() {
            long deckId = createDeck(registerNew().accessToken(), "Private", "PRIVATE");
            Session stranger = registerNew();

            MvcTestResult result = postJson(cards(deckId), stranger.accessToken(), """
                    {"word": "spam", "meaning": "rác"}
                    """);

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class Read {

        @Test
        void listsCardsAlphabeticallyWithPaging() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Fruits", "PRIVATE");
            createCard(session.accessToken(), deckId, "cherry", "quả anh đào");
            createCard(session.accessToken(), deckId, "Apple", "quả táo");
            createCard(session.accessToken(), deckId, "banana", "quả chuối");

            MvcTestResult result = getWithToken(cards(deckId) + "?page=0&size=2", session.accessToken());

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.items[0].word").isEqualTo("Apple");
            assertThat(result).bodyJson().extractingPath("$.items[1].word").isEqualTo("banana");
            assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(3);
            assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
        }

        @Test
        void searchesWordAndMeaningLiterally() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Mixed", "PRIVATE");
            createCard(session.accessToken(), deckId, "sunny", "có nắng");
            createCard(session.accessToken(), deckId, "rain", "mưa");
            createCard(session.accessToken(), deckId, "100%", "một trăm phần trăm");

            MvcTestResult byWord = getWithToken(cards(deckId) + "?q=SUN", session.accessToken());
            MvcTestResult byMeaning = getWithToken(cards(deckId) + "?q=mưa", session.accessToken());
            MvcTestResult wildcard = mvc.get().uri(cards(deckId)).param("q", "%")
                    .header("Authorization", "Bearer " + session.accessToken()).exchange();

            assertThat(byWord).bodyJson().extractingPath("$.items[*].word").asArray().containsExactly("sunny");
            assertThat(byMeaning).bodyJson().extractingPath("$.items[*].word").asArray().containsExactly("rain");
            assertThat(wildcard).bodyJson().extractingPath("$.items[*].word").asArray().containsExactly("100%");
        }

        @Test
        void rejectsPageSizeAbove100() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Big", "PRIVATE");

            MvcTestResult result = getWithToken(cards(deckId) + "?size=101", session.accessToken());

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("size");
        }

        @Test
        void otherUserCanReadCardsOfPublicDeckButNotPrivate() {
            Session owner = registerNew();
            long publicDeck = createDeck(owner.accessToken(), "Open", "PUBLIC");
            long privateDeck = createDeck(owner.accessToken(), "Closed", "PRIVATE");
            long cardId = createCard(owner.accessToken(), privateDeck, "secret", "bí mật");
            Session stranger = registerNew();

            assertThat(getWithToken(cards(publicDeck), stranger.accessToken())).hasStatusOk();
            assertThat(getWithToken(cards(privateDeck), stranger.accessToken())).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(getWithToken(cards(privateDeck) + "/" + cardId, stranger.accessToken()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void cardOfAnotherDeckIsNotFound() {
            Session session = registerNew();
            long deckA = createDeck(session.accessToken(), "A", "PRIVATE");
            long deckB = createDeck(session.accessToken(), "B", "PRIVATE");
            long cardInA = createCard(session.accessToken(), deckA, "word", "từ");

            MvcTestResult result = getWithToken(cards(deckB) + "/" + cardInA, session.accessToken());

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class UpdateAndDelete {

        @Test
        void ownerEditsCard() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Edit", "PRIVATE");
            long cardId = createCard(session.accessToken(), deckId, "colour", "màu");

            MvcTestResult result = putJson(cards(deckId) + "/" + cardId, session.accessToken(), """
                    {"word": "Colour", "meaning": "màu sắc", "partOfSpeech": "noun"}
                    """);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.word").isEqualTo("Colour");
            assertThat(result).bodyJson().extractingPath("$.meaning").isEqualTo("màu sắc");
        }

        @Test
        void editingToAnExistingWordConflicts_R2() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Dup", "PRIVATE");
            createCard(session.accessToken(), deckId, "big", "to");
            long cardId = createCard(session.accessToken(), deckId, "large", "lớn");

            MvcTestResult result = putJson(cards(deckId) + "/" + cardId, session.accessToken(), """
                    {"word": "BIG", "meaning": "lớn"}
                    """);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
        }

        @Test
        void otherUserCannotEditOrDeleteCardsOfPublicDeck_R3() {
            Session owner = registerNew();
            long deckId = createDeck(owner.accessToken(), "Guarded", "PUBLIC");
            long cardId = createCard(owner.accessToken(), deckId, "guard", "bảo vệ");
            Session stranger = registerNew();

            assertThat(putJson(cards(deckId) + "/" + cardId, stranger.accessToken(), """
                    {"word": "x", "meaning": "y"}
                    """)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(deleteWithToken(cards(deckId) + "/" + cardId, stranger.accessToken()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void deletedCardDisappearsAndItsWordCanBeReused() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Trim", "PRIVATE");
            long cardId = createCard(session.accessToken(), deckId, "typo", "lỗi đánh máy");

            MvcTestResult result = deleteWithToken(cards(deckId) + "/" + cardId, session.accessToken());

            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(getWithToken(cards(deckId) + "/" + cardId, session.accessToken()))
                    .hasStatus(HttpStatus.NOT_FOUND);
            assertThat(getWithToken("/api/decks/" + deckId, session.accessToken())).bodyJson()
                    .extractingPath("$.cardCount").isEqualTo(0);
            createCard(session.accessToken(), deckId, "typo", "lỗi đánh máy");
        }
    }
}
