-- M3: each learner's SM-2 state per card, and the immutable review history (R5, R7, R8).

CREATE TABLE card_progress (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT           NOT NULL REFERENCES users (id),
    card_id          BIGINT           NOT NULL REFERENCES cards (id),
    easiness_factor  DOUBLE PRECISION NOT NULL,
    repetitions      INT              NOT NULL,
    interval_days    INT              NOT NULL,
    state            VARCHAR(20)      NOT NULL,
    lapses           INT              NOT NULL,
    next_review_at   TIMESTAMPTZ,
    last_reviewed_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ      NOT NULL,
    updated_at       TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uk_card_progress_user_card UNIQUE (user_id, card_id),
    CONSTRAINT ck_card_progress_state CHECK (state IN ('NEW', 'LEARNING', 'REVIEW')),
    -- R5
    CONSTRAINT ck_card_progress_ef CHECK (easiness_factor >= 1.3),
    CONSTRAINT ck_card_progress_counters CHECK (repetitions >= 0 AND interval_days >= 0 AND lapses >= 0),
    CONSTRAINT ck_card_progress_scheduled CHECK (state = 'NEW' OR (next_review_at IS NOT NULL AND interval_days >= 1))
);

-- due cards of a learner: WHERE user_id = ? AND next_review_at < ?
CREATE INDEX ix_card_progress_user_next_review ON card_progress (user_id, next_review_at);

CREATE TABLE review_logs (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_progress_id BIGINT           NOT NULL REFERENCES card_progress (id),
    user_id          BIGINT           NOT NULL REFERENCES users (id),
    card_id          BIGINT           NOT NULL REFERENCES cards (id),
    quality          INT              NOT NULL,
    state_before     VARCHAR(20)      NOT NULL,
    state_after      VARCHAR(20)      NOT NULL,
    ef_before        DOUBLE PRECISION NOT NULL,
    ef_after         DOUBLE PRECISION NOT NULL,
    interval_before  INT              NOT NULL,
    interval_after   INT              NOT NULL,
    reviewed_at      TIMESTAMPTZ      NOT NULL,
    CONSTRAINT ck_review_logs_quality CHECK (quality BETWEEN 0 AND 5)
);

-- daily counts (R7 new cards per day, R9 streak): WHERE user_id = ? AND reviewed_at in [from, to)
CREATE INDEX ix_review_logs_user_reviewed_at ON review_logs (user_id, reviewed_at);

-- R8: the review history is append-only, even for direct SQL
CREATE FUNCTION forbid_review_log_change() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'review_logs is append-only (R8)';
END;
$$;

CREATE TRIGGER trg_review_logs_append_only
    BEFORE UPDATE OR DELETE ON review_logs
    FOR EACH ROW EXECUTE FUNCTION forbid_review_log_change();
