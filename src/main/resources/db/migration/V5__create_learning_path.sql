-- Learning path (business-spec §6, R10–R12) and the deck subscriptions it needs (R4, R6).

-- UC-L7: a learner studies a PUBLIC / OFFICIAL deck they do not own. Starting a lesson subscribes its deck.
CREATE TABLE deck_subscriptions (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    deck_id    BIGINT      NOT NULL REFERENCES decks (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_deck_subscriptions_user_deck UNIQUE (user_id, deck_id)
);

CREATE INDEX ix_deck_subscriptions_deck ON deck_subscriptions (deck_id);

CREATE TABLE learning_paths (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE path_stages (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    path_id    BIGINT       NOT NULL REFERENCES learning_paths (id),
    cefr_level VARCHAR(2)   NOT NULL,
    title      VARCHAR(100) NOT NULL,
    position   INT          NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_path_stages_cefr CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    CONSTRAINT uk_path_stages_position UNIQUE (path_id, position),
    CONSTRAINT uk_path_stages_cefr UNIQUE (path_id, cefr_level)
);

-- R10: a lesson is one OFFICIAL deck, and a deck belongs to at most one lesson
CREATE TABLE path_lessons (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stage_id   BIGINT      NOT NULL REFERENCES path_stages (id),
    deck_id    BIGINT      NOT NULL REFERENCES decks (id),
    position   INT         NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_path_lessons_deck UNIQUE (deck_id),
    CONSTRAINT ck_path_lessons_position CHECK (position >= 1)
);

CREATE INDEX ix_path_lessons_stage ON path_lessons (stage_id, position);

-- R11: once a stage is open for a learner it stays open
CREATE TABLE stage_unlocks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    stage_id    BIGINT      NOT NULL REFERENCES path_stages (id),
    method      VARCHAR(20) NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_stage_unlocks_user_stage UNIQUE (user_id, stage_id),
    CONSTRAINT ck_stage_unlocks_method CHECK (method IN ('LESSONS', 'TEST'))
);

-- R12: the stage test
CREATE TABLE stage_test_attempts (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES users (id),
    stage_id      BIGINT      NOT NULL REFERENCES path_stages (id),
    started_at    TIMESTAMPTZ NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    submitted_at  TIMESTAMPTZ,
    correct_count INT,
    passed        BOOLEAN,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_stage_test_attempts_result CHECK (
        (submitted_at IS NULL AND correct_count IS NULL AND passed IS NULL)
        OR (submitted_at IS NOT NULL AND correct_count IS NOT NULL AND passed IS NOT NULL))
);

-- latest attempt of a learner for a stage (cooldown, attempt in progress)
CREATE INDEX ix_stage_test_attempts_user_stage ON stage_test_attempts (user_id, stage_id, started_at DESC);

CREATE TABLE stage_test_questions (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attempt_id BIGINT       NOT NULL REFERENCES stage_test_attempts (id),
    card_id    BIGINT       NOT NULL REFERENCES cards (id),
    position   INT          NOT NULL,
    answer     VARCHAR(100),
    correct    BOOLEAN,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_stage_test_questions_position UNIQUE (attempt_id, position)
);

-- Reference data: the CEFR path and its six stages. Lessons (OFFICIAL decks) are added by admins.
INSERT INTO learning_paths (name, description, created_at, updated_at)
VALUES ('Lộ trình CEFR', 'Sáu chặng từ A1 đến C2', now(), now());

INSERT INTO path_stages (path_id, cefr_level, title, position, created_at, updated_at)
SELECT p.id, s.cefr_level, s.title, s.position, now(), now()
FROM learning_paths p
CROSS JOIN (VALUES ('A1', 'Trình độ căn bản', 1),
                   ('A2', 'Trình độ sơ cấp', 2),
                   ('B1', 'Trình độ trung cấp', 3),
                   ('B2', 'Trình độ trung cao cấp', 4),
                   ('C1', 'Trình độ cao cấp', 5),
                   ('C2', 'Trình độ thành thạo', 6)) AS s (cefr_level, title, position)
WHERE p.name = 'Lộ trình CEFR';
