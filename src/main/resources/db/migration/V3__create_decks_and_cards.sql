-- M2: decks and cards (R2, R3, R8).
-- Both are soft-deleted (deleted_at) so that learning history (M3 ReviewLog) is never lost.

CREATE TABLE decks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES users (id),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    visibility  VARCHAR(20)  NOT NULL,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_decks_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC', 'OFFICIAL'))
);

-- R2: deck names are unique per owner (case-insensitive) among decks that are not deleted
CREATE UNIQUE INDEX uk_decks_owner_name ON decks (owner_id, lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE cards (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    deck_id        BIGINT       NOT NULL REFERENCES decks (id),
    word           VARCHAR(100) NOT NULL,
    meaning        VARCHAR(500) NOT NULL,
    ipa            VARCHAR(100),
    part_of_speech VARCHAR(30),
    example        VARCHAR(500),
    note           VARCHAR(500),
    deleted_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);

-- R2: a word is unique inside a deck (case-insensitive) among cards that are not deleted
CREATE UNIQUE INDEX uk_cards_deck_word ON cards (deck_id, lower(word)) WHERE deleted_at IS NULL;
