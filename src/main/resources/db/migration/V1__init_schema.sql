-- V1: baseline migration.
-- Real tables (users, decks, cards, card_progress, review_logs...) are added
-- milestone by milestone as new versioned scripts (V2__, V3__, ...).
-- Never edit a migration that has already been applied; add a new one instead.

CREATE TABLE IF NOT EXISTS app_metadata
(
    meta_key   VARCHAR(100) PRIMARY KEY,
    meta_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO app_metadata (meta_key, meta_value)
VALUES ('schema_baseline', 'M0 - project setup');
