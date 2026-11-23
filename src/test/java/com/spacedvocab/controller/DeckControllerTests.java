package com.spacedvocab.controller;

import com.spacedvocab.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class DeckControllerTests extends ApiTestSupport {

    @Nested
    class Create {

        @Test
        void createsPrivateDeckOwnedByTheCaller() {
            Session session = registerNew();

            MvcTestResult result = postJson("/api/decks", session.accessToken(), """
                    {"name": "  IELTS Band 7  ", "description": "Từ vựng học thuật", "visibility": "PRIVATE"}
                    """);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("IELTS Band 7");
            assertThat(result).bodyJson().extractingPath("$.visibility").isEqualTo("PRIVATE");
            assertThat(result).bodyJson().extractingPath("$.cardCount").isEqualTo(0);
            assertThat(result).bodyJson().extractingPath("$.editable").isEqualTo(true);
            assertThat(result).bodyJson().extractingPath("$.owner.displayName").isNotNull();
        }

        @Test
        void rejectsBlankNameAndMissingVisibility() {
            Session session = registerNew();

            MvcTestResult result = postJson("/api/decks", session.accessToken(), """
                    {"name": "   "}
                    """);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
            assertThat(result).bodyJson().extractingPath("$.errors.length()").isEqualTo(2);
        }

        @Test
        void rejectsNameLongerThan100Characters() {
            Session session = registerNew();

            MvcTestResult result = postJson("/api/decks", session.accessToken(), """
                    {"name": "%s", "visibility": "PRIVATE"}
                    """.formatted("a".repeat(101)));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("name");
        }

        @Test
        void rejectsDuplicateNameOfTheSameOwnerIgnoringCase_R2() {
            Session session = registerNew();
            createDeck(session.accessToken(), "Oxford 3000", "PRIVATE");

            MvcTestResult result = postJson("/api/decks", session.accessToken(), """
                    {"name": "oxford 3000", "visibility": "PUBLIC"}
                    """);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("DECK_NAME_TAKEN");
            assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("name");
        }

        @Test
        void allowsTheSameNameForDifferentOwners_R2() {
            createDeck(registerNew().accessToken(), "Daily words", "PRIVATE");

            createDeck(registerNew().accessToken(), "Daily words", "PRIVATE");
        }

        @Test
        void learnerCannotCreateOfficialDeck_R3() {
            Session session = registerNew();

            MvcTestResult result = postJson("/api/decks", session.accessToken(), """
                    {"name": "Fake official", "visibility": "OFFICIAL"}
                    """);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
        }

        @Test
        void adminCannotCreateLearnerDecks() {
            Session admin = toSession("admin@test.local", login("admin@test.local", "admin-test-password"));

            MvcTestResult result = postJson("/api/decks", admin.accessToken(), """
                    {"name": "Admin deck", "visibility": "PRIVATE"}
                    """);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void requiresAuthentication() {
            MvcTestResult result = postJson("/api/decks", """
                    {"name": "No token", "visibility": "PRIVATE"}
                    """);

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    class Read {

        @Test
        void listsOnlyTheCallersDecksWithCardCount() {
            Session me = registerNew();
            Session other = registerNew();
            long deckId = createDeck(me.accessToken(), "Mine", "PRIVATE");
            createCard(me.accessToken(), deckId, "apple", "quả táo");
            createCard(me.accessToken(), deckId, "banana", "quả chuối");
            createDeck(other.accessToken(), "Not mine", "PUBLIC");

            MvcTestResult result = getWithToken("/api/decks?scope=owned", me.accessToken());

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(1);
            assertThat(result).bodyJson().extractingPath("$[0].name").isEqualTo("Mine");
            assertThat(result).bodyJson().extractingPath("$[0].cardCount").isEqualTo(2);
        }

        @Test
        void rejectsUnsupportedScope() {
            Session session = registerNew();

            MvcTestResult result = getWithToken("/api/decks?scope=everything", session.accessToken());

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("scope");
        }

        @Test
        void otherUsersPrivateDeckIsNotFound() {
            long deckId = createDeck(registerNew().accessToken(), "Secret", "PRIVATE");
            Session stranger = registerNew();

            MvcTestResult result = getWithToken("/api/decks/" + deckId, stranger.accessToken());

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
        }

        @Test
        void otherUsersPublicDeckIsReadOnly() {
            long deckId = createDeck(registerNew().accessToken(), "Shared", "PUBLIC");
            Session stranger = registerNew();

            MvcTestResult result = getWithToken("/api/decks/" + deckId, stranger.accessToken());

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.editable").isEqualTo(false);
        }

        @Test
        void nonNumericIdIsAValidationError() {
            Session session = registerNew();

            MvcTestResult result = getWithToken("/api/decks/abc", session.accessToken());

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.field").isEqualTo("deckId");
        }
    }

    @Nested
    class Update {

        @Test
        void ownerUpdatesDeck() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Draft", "PRIVATE");

            MvcTestResult result = putJson("/api/decks/" + deckId, session.accessToken(), """
                    {"name": "Travel English", "description": "  ", "visibility": "PUBLIC"}
                    """);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Travel English");
            assertThat(result).bodyJson().extractingPath("$.visibility").isEqualTo("PUBLIC");
            assertThat(result).bodyJson().extractingPath("$.description").isNull();
        }

        @Test
        void keepingTheSameNameWithDifferentCaseIsAllowed_R2() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "verbs", "PRIVATE");

            MvcTestResult result = putJson("/api/decks/" + deckId, session.accessToken(), """
                    {"name": "Verbs", "visibility": "PRIVATE"}
                    """);

            assertThat(result).hasStatusOk();
        }

        @Test
        void renamingToAnotherOwnDeckNameConflicts_R2() {
            Session session = registerNew();
            createDeck(session.accessToken(), "Nouns", "PRIVATE");
            long deckId = createDeck(session.accessToken(), "Adjectives", "PRIVATE");

            MvcTestResult result = putJson("/api/decks/" + deckId, session.accessToken(), """
                    {"name": "NOUNS", "visibility": "PRIVATE"}
                    """);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("DECK_NAME_TAKEN");
        }

        @Test
        void otherUserCannotEditPublicDeck_R3() {
            long deckId = createDeck(registerNew().accessToken(), "Public deck", "PUBLIC");
            Session stranger = registerNew();

            MvcTestResult result = putJson("/api/decks/" + deckId, stranger.accessToken(), """
                    {"name": "Hijacked", "visibility": "PUBLIC"}
                    """);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void otherUserEditingPrivateDeckGetsNotFound_R3() {
            long deckId = createDeck(registerNew().accessToken(), "Private deck", "PRIVATE");
            Session stranger = registerNew();

            MvcTestResult result = putJson("/api/decks/" + deckId, stranger.accessToken(), """
                    {"name": "Hijacked", "visibility": "PRIVATE"}
                    """);

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void ownerCannotTurnDeckOfficial_R3() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Wannabe", "PUBLIC");

            MvcTestResult result = putJson("/api/decks/" + deckId, session.accessToken(), """
                    {"name": "Wannabe", "visibility": "OFFICIAL"}
                    """);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletedDeckDisappearsAndItsNameCanBeReused_R8() {
            Session session = registerNew();
            long deckId = createDeck(session.accessToken(), "Old deck", "PUBLIC");
            createCard(session.accessToken(), deckId, "old", "cũ");

            MvcTestResult result = deleteWithToken("/api/decks/" + deckId, session.accessToken());

            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(getWithToken("/api/decks/" + deckId, session.accessToken())).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(getWithToken("/api/decks/%d/cards".formatted(deckId), session.accessToken()))
                    .hasStatus(HttpStatus.NOT_FOUND);
            assertThat(getWithToken("/api/decks", session.accessToken())).bodyJson()
                    .extractingPath("$.length()").isEqualTo(0);
            createDeck(session.accessToken(), "Old deck", "PUBLIC");
        }

        @Test
        void otherUserCannotDeletePublicDeck_R3() {
            long deckId = createDeck(registerNew().accessToken(), "Keep me", "PUBLIC");
            Session stranger = registerNew();

            MvcTestResult result = deleteWithToken("/api/decks/" + deckId, stranger.accessToken());

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        }
    }
}
