-- NewsCheck Database Initialization
-- This runs once when the PostgreSQL container first starts.

-- Aggregator tables (owned by the Aggregator service)
CREATE TABLE IF NOT EXISTS articles (
    id            BIGSERIAL PRIMARY KEY,
    external_id   VARCHAR(512) UNIQUE NOT NULL,   -- URL or API-provided ID
    title         TEXT          NOT NULL,
    description   TEXT,
    content       TEXT,
    url           VARCHAR(1024) NOT NULL,
    image_url     VARCHAR(1024),
    source_name   VARCHAR(255),
    author        VARCHAR(255),
    category      VARCHAR(100)  NOT NULL DEFAULT 'general',
    published_at  TIMESTAMPTZ   NOT NULL,
    fetched_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    is_breaking   BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_articles_category      ON articles(category);
CREATE INDEX IF NOT EXISTS idx_articles_published_at  ON articles(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_articles_is_breaking   ON articles(is_breaking);

-- GIN index for full-text search on title + description
-- Uses english dictionary for stemming (e.g., "running" matches "run")
CREATE INDEX IF NOT EXISTS idx_articles_fts
    ON articles USING GIN (
        to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(description, ''))
    );

-- News-Server tables (owned by the News Server service)
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) UNIQUE NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255)        NOT NULL,
    fcm_token     VARCHAR(512),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category   VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, category)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id  ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_category ON subscriptions(category);

CREATE TABLE IF NOT EXISTS read_articles (
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    read_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, article_id)
);

-- Seed a test user (password: "password123" bcrypt hash)
INSERT INTO users (username, email, password_hash)
VALUES ('testuser', 'test@newscheck.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y')
ON CONFLICT DO NOTHING;
