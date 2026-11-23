package com.spacedvocab.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DeckTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_ID = 2L;

    private static Deck deck(DeckVisibility visibility) {
        User owner = User.newLearner("owner@example.com", "{noop}x", "Owner", User.DEFAULT_TIMEZONE);
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        return new Deck(owner, " Name ", " ", visibility);
    }

    @Test
    void trimsNameAndTurnsBlankDescriptionIntoNull() {
        Deck deck = deck(DeckVisibility.PRIVATE);

        assertThat(deck.getName()).isEqualTo("Name");
        assertThat(deck.getDescription()).isNull();
    }

    @Test
    void rejectsBlankName() {
        Deck deck = deck(DeckVisibility.PRIVATE);

        assertThatIllegalArgumentException().isThrownBy(() -> deck.edit("  ", null, DeckVisibility.PRIVATE));
    }

    @Test
    void privateDeckIsVisibleAndEditableOnlyByItsOwner() {
        Deck deck = deck(DeckVisibility.PRIVATE);

        assertThat(deck.isOwnedBy(OWNER_ID)).isTrue();
        assertThat(deck.isVisibleTo(OWNER_ID)).isTrue();
        assertThat(deck.isVisibleTo(OTHER_ID)).isFalse();
        assertThat(deck.isEditableBy(OWNER_ID, Role.LEARNER)).isTrue();
        assertThat(deck.isEditableBy(OTHER_ID, Role.ADMIN)).isFalse();
    }

    @Test
    void publicDeckIsVisibleToEveryoneButEditableOnlyByItsOwner_R3() {
        Deck deck = deck(DeckVisibility.PUBLIC);

        assertThat(deck.isVisibleTo(OTHER_ID)).isTrue();
        assertThat(deck.isEditableBy(OTHER_ID, Role.LEARNER)).isFalse();
        assertThat(deck.isEditableBy(OWNER_ID, Role.LEARNER)).isTrue();
    }

    @Test
    void officialDeckIsEditableByAdminsOnly_R3() {
        Deck deck = deck(DeckVisibility.OFFICIAL);

        assertThat(deck.isEditableBy(OTHER_ID, Role.ADMIN)).isTrue();
        assertThat(deck.isEditableBy(OWNER_ID, Role.LEARNER)).isFalse();
    }

    @Test
    void deletedDeckIsNeitherVisibleNorEditable_R8() {
        Deck deck = deck(DeckVisibility.PUBLIC);
        Instant now = Instant.parse("2026-11-23T03:00:00Z");

        deck.delete(now);
        deck.delete(now.plusSeconds(60));

        assertThat(deck.getDeletedAt()).isEqualTo(now);
        assertThat(deck.isVisibleTo(OWNER_ID)).isFalse();
        assertThat(deck.isEditableBy(OWNER_ID, Role.LEARNER)).isFalse();
    }
}
