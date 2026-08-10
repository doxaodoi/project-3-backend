-- V1 — Reclaim initial schema
-- CPEN 208 Project 3 — Campus Lost & Found

-- ============================================================
-- Users
-- ============================================================
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(120)  NOT NULL,
    email         VARCHAR(255)  NOT NULL UNIQUE,
    phone         VARCHAR(30),
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL DEFAULT 'USER',
    avatar_url    VARCHAR(512),
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT now()
);

-- ============================================================
-- Categories & Locations (admin-managed reference tables)
-- ============================================================
CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(60)  NOT NULL UNIQUE,
    icon VARCHAR(60)
);

CREATE TABLE locations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION
);

-- ============================================================
-- Items (lost / found reports)
-- ============================================================
CREATE TABLE items (
    id              BIGSERIAL PRIMARY KEY,
    reporter_id     BIGINT        NOT NULL REFERENCES users(id),
    type            VARCHAR(10)   NOT NULL CHECK (type IN ('LOST', 'FOUND')),
    title           VARCHAR(200)  NOT NULL,
    description     TEXT,
    category_id     BIGINT        REFERENCES categories(id),
    location_id     BIGINT        REFERENCES locations(id),
    held_at         VARCHAR(200),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    event_date      DATE,
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN','MATCHED','RESOLVED','ARCHIVED')),
    color           VARCHAR(60),
    brand           VARCHAR(100),
    is_ai_described BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_items_type       ON items(type);
CREATE INDEX idx_items_status     ON items(status);
CREATE INDEX idx_items_category   ON items(category_id);
CREATE INDEX idx_items_location   ON items(location_id);
CREATE INDEX idx_items_reporter   ON items(reporter_id);
CREATE INDEX idx_items_created    ON items(created_at DESC);

-- ============================================================
-- Item photos
-- ============================================================
CREATE TABLE item_photos (
    id         BIGSERIAL PRIMARY KEY,
    item_id    BIGINT       NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    url        TEXT         NOT NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- ============================================================
-- Tags (many-to-many with items)
-- ============================================================
CREATE TABLE tags (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE item_tags (
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,
    PRIMARY KEY (item_id, tag_id)
);

-- ============================================================
-- Claims
-- ============================================================
CREATE TABLE claims (
    id          BIGSERIAL PRIMARY KEY,
    item_id     BIGINT      NOT NULL REFERENCES items(id),
    claimant_id BIGINT      NOT NULL REFERENCES users(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN')),
    message     TEXT,
    reviewed_by BIGINT      REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_claims_item     ON claims(item_id);
CREATE INDEX idx_claims_claimant ON claims(claimant_id);

-- ============================================================
-- Matches (suggested links between a lost and a found item)
-- ============================================================
CREATE TABLE matches (
    id             BIGSERIAL PRIMARY KEY,
    lost_item_id   BIGINT         NOT NULL REFERENCES items(id),
    found_item_id  BIGINT         NOT NULL REFERENCES items(id),
    score          DOUBLE PRECISION NOT NULL DEFAULT 0,
    ai_explanation TEXT,
    status         VARCHAR(20)    NOT NULL DEFAULT 'SUGGESTED'
                       CHECK (status IN ('SUGGESTED','CONFIRMED','DISMISSED')),
    created_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_matches_lost  ON matches(lost_item_id);
CREATE INDEX idx_matches_found ON matches(found_item_id);

-- ============================================================
-- Conversations & Messages (in-app private messaging)
-- ============================================================
CREATE TABLE conversations (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT    REFERENCES items(id),
    claim_id        BIGINT    REFERENCES claims(id),
    user_a_id       BIGINT    NOT NULL REFERENCES users(id),
    user_b_id       BIGINT    NOT NULL REFERENCES users(id),
    last_message_at TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT    NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       BIGINT    NOT NULL REFERENCES users(id),
    body            TEXT      NOT NULL,
    is_read         BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);

-- ============================================================
-- Notifications
-- ============================================================
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    type       VARCHAR(40)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       TEXT,
    link       VARCHAR(512),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read, created_at DESC);

-- ============================================================
-- Seed: categories & locations (UG Legon campus)
-- ============================================================
INSERT INTO categories (name, icon) VALUES
    ('Electronics',      'laptop'),
    ('Keys & Access',    'key'),
    ('Bags & Wallets',   'briefcase'),
    ('Clothing',         'shirt'),
    ('Books & Notes',    'book'),
    ('ID & Documents',   'id-card'),
    ('Water Bottles',    'droplet'),
    ('Jewelry',          'gem'),
    ('Sports Equipment', 'dumbbell'),
    ('Other',            'box');

INSERT INTO locations (name, latitude, longitude) VALUES
    ('Balme Library',       5.6510, -0.1866),
    ('Bush Canteen',        5.6535, -0.1890),
    ('JQB (Dept. of CS)',   5.6545, -0.1870),
    ('Great Hall',          5.6500, -0.1895),
    ('Main Gate',           5.6560, -0.1880),
    ('Legon Hall',          5.6490, -0.1860),
    ('Akuafo Hall',         5.6520, -0.1850),
    ('Pentagon Hall',       5.6530, -0.1900),
    ('N Block',             5.6555, -0.1875),
    ('Security Office',     5.6560, -0.1869),
    ('Science Block',       5.6540, -0.1855),
    ('Athletic Oval',       5.6515, -0.1910);
