-- ============================================================
-- Reclaim — Campus Lost & Found  |  Full database backup/export
-- CPEN 208 Software Engineering — Project 3 (Group 1)
--
-- This single script recreates the complete PostgreSQL database:
--   schema (tables, keys, indexes) + seed/demo data + data fixes.
-- It is the ordered concatenation of the Flyway migrations V1..V8,
-- i.e. exactly what the application applies on first boot.
--
-- Restore:  psql -U <user> -d <db> -f reclaim_full_backup.sql
-- Or via Docker: see database/README.md
-- ============================================================


-- ------------------------------------------------------------
-- V1__initial_schema.sql
-- ------------------------------------------------------------
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


-- ------------------------------------------------------------
-- V2__seed_demo_data.sql
-- ------------------------------------------------------------
-- V2 — Seed demo data with realistic items and web images
-- Passwords are BCrypt hashes of "Password123"

-- ============================================================
-- Users (1 admin + 4 regular users)
-- ============================================================
INSERT INTO users (full_name, email, phone, password_hash, role) VALUES
    ('Admin Reclaim',     'admin@reclaim.app',     '0201234567',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
    ('Ama Mensah',        'ama.mensah@ug.edu.gh',  '0551234567',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
    ('Kwame Asante',      'kwame.asante@ug.edu.gh','0241234567',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
    ('Efua Darkwa',       'efua.darkwa@ug.edu.gh', '0271234567',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
    ('Kofi Boateng',      'kofi.boateng@ug.edu.gh','0501234567',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER');

-- ============================================================
-- Items (mix of LOST and FOUND with real image URLs)
-- ============================================================

-- Item 1: Lost iPhone 14 Pro (Ama, Electronics, Balme Library)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (2, 'LOST', 'iPhone 14 Pro — Space Black',
        'Lost my iPhone 14 Pro in a black silicone case near the reading area on the second floor of Balme Library. It has a cracked screen protector and a red UG lanyard attached. Last seen around 2pm during a study session.',
        1, 1, NULL, 5.6510, -0.1866, '2026-08-05', 'MATCHED', 'Black', 'Apple', false);

-- Item 2: Found iPhone (Kwame, Electronics, Balme Library)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (3, 'FOUND', 'Black iPhone in Silicone Case',
        'Found a black iPhone with a cracked screen protector on a desk in Balme Library second floor. Has a red lanyard. Currently being held at the library front desk.',
        1, 1, 'Balme Library Front Desk', 5.6512, -0.1864, '2026-08-05', 'MATCHED', 'Black', 'Apple', true);

-- Item 3: Lost Keys (Efua, Keys & Access, Bush Canteen)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (4, 'LOST', 'Bunch of Keys with Red Lanyard',
        'Lost a set of 4 keys on a red UG-branded lanyard somewhere around Bush Canteen. Includes my room key, padlock key, and two smaller keys. Really need these back.',
        2, 2, NULL, 5.6535, -0.1890, '2026-08-07', 'OPEN', 'Red', NULL, false);

-- Item 4: Found Keys (Kofi, Keys & Access, Great Hall)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (5, 'FOUND', 'Set of Keys on Lanyard',
        'Found a bunch of keys on a red lanyard near the Great Hall entrance. About 4-5 keys including what looks like a padlock key. Turned in to Security Office.',
        2, 4, 'Security Office', 5.6502, -0.1893, '2026-08-07', 'OPEN', 'Red', NULL, false);

-- Item 5: Lost Backpack (Kwame, Bags & Wallets, JQB)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (3, 'LOST', 'Navy Blue Laptop Backpack',
        'Left my navy blue HP laptop backpack in JQB Room 102 after the CPEN 208 lecture. Contains my laptop, charger, notebooks, and a calculator. The bag has a small tear on the front pocket.',
        3, 3, NULL, 5.6545, -0.1870, '2026-08-08', 'OPEN', 'Navy Blue', 'HP', false);

-- Item 6: Found Backpack (Ama, Bags & Wallets, JQB)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (2, 'FOUND', 'Blue Laptop Bag Found in JQB',
        'Found a blue laptop backpack left behind in JQB after the afternoon lecture. It has a laptop inside and some notebooks. There is a small rip on the front pocket. Holding it at the CS department office.',
        3, 3, 'CS Department Office (JQB)', 5.6544, -0.1871, '2026-08-08', 'OPEN', 'Blue', NULL, true);

-- Item 7: Lost Student ID (Efua, ID & Documents, Legon Hall)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (4, 'LOST', 'UG Student ID Card — Efua Darkwa',
        'Lost my UG student ID card somewhere between Legon Hall and the Athletic Oval. I need it to access the library and write exams. Name on card: Efua Darkwa, ID: 22315678.',
        6, 6, NULL, 5.6492, -0.1862, '2026-08-09', 'OPEN', NULL, NULL, false);

-- Item 8: Found Water Bottle (Kofi, Water Bottles, Athletic Oval)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (5, 'FOUND', 'Blue Hydro Flask Water Bottle',
        'Found a blue Hydro Flask water bottle near the bleachers at the Athletic Oval after the evening jog session. Has some stickers on it including a Ghana flag sticker.',
        7, 12, 'Athletic Oval Lost Items Box', 5.6515, -0.1910, '2026-08-09', 'OPEN', 'Blue', 'Hydro Flask', true);

-- Item 9: Lost Textbook (Ama, Books & Notes, N Block)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (2, 'LOST', 'Engineering Mathematics Textbook',
        'Left my Engineering Mathematics textbook (Stroud, 8th edition) in N Block Lecture Hall after the morning class. It has my name written inside the cover and several sticky notes.',
        5, 9, NULL, 5.6555, -0.1875, '2026-08-10', 'OPEN', NULL, NULL, false);

-- Item 10: Found Earbuds (Kwame, Electronics, Pentagon Hall)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (3, 'FOUND', 'White AirPods Pro in Charging Case',
        'Found white AirPods Pro in their charging case on a bench outside Pentagon Hall. The case has a small scratch on the back. They are still charged and working.',
        1, 8, 'Pentagon Hall Porter Lodge', 5.6530, -0.1900, '2026-08-10', 'OPEN', 'White', 'Apple', false);

-- Item 11: Lost Wallet (Kofi, Bags & Wallets, Main Gate)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (5, 'LOST', 'Brown Leather Wallet',
        'Lost my brown leather wallet near Main Gate, probably dropped it getting off the trotro. Contains my Ghana card, ATM cards (already blocked), and about GHS 50. No brand marking.',
        3, 5, NULL, 5.6560, -0.1880, '2026-08-09', 'OPEN', 'Brown', NULL, false);

-- Item 12: Found Jacket (Efua, Clothing, Science Block)
INSERT INTO items (reporter_id, type, title, description, category_id, location_id, held_at,
                   latitude, longitude, event_date, status, color, brand, is_ai_described)
VALUES (4, 'FOUND', 'Black Denim Jacket — Size M',
        'Found a black denim jacket left on a chair in the Science Block computer lab. Size medium, no name tag. Has a small pin on the collar.',
        4, 11, 'Science Block Lab Attendant', 5.6540, -0.1855, '2026-08-08', 'OPEN', 'Black', NULL, false);

-- ============================================================
-- Photos (real web images matching the items)
-- ============================================================
INSERT INTO item_photos (item_id, url, is_primary) VALUES
    -- iPhone Lost
    (1, 'https://images.unsplash.com/photo-1678685888221-cda773a3dcdb?w=600&h=400&fit=crop', true),
    -- iPhone Found
    (2, 'https://images.unsplash.com/photo-1695048065319-e6073f9af61c?w=600&h=400&fit=crop', true),
    -- Keys Lost
    (3, 'https://images.unsplash.com/photo-1582139329536-e7284fece509?w=600&h=400&fit=crop', true),
    -- Keys Found
    (4, 'https://images.unsplash.com/photo-1609151354296-e82e1e3e7c1d?w=600&h=400&fit=crop', true),
    -- Backpack Lost
    (5, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&h=400&fit=crop', true),
    -- Backpack Found
    (6, 'https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=600&h=400&fit=crop', true),
    -- Student ID Lost
    (7, 'https://images.unsplash.com/photo-1578670812003-60745e2c2ea9?w=600&h=400&fit=crop', true),
    -- Water Bottle Found
    (8, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600&h=400&fit=crop', true),
    -- Textbook Lost
    (9, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&h=400&fit=crop', true),
    -- AirPods Found
    (10, 'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=600&h=400&fit=crop', true),
    -- Wallet Lost
    (11, 'https://images.unsplash.com/photo-1627123424574-724758594e93?w=600&h=400&fit=crop', true),
    -- Jacket Found
    (12, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=600&h=400&fit=crop', true);

-- ============================================================
-- Tags
-- ============================================================
INSERT INTO tags (name) VALUES
    ('phone'), ('apple'), ('black'), ('cracked screen'), ('lanyard'),
    ('keys'), ('red'), ('padlock'), ('backpack'), ('laptop'),
    ('hp'), ('blue'), ('id card'), ('student'), ('water bottle'),
    ('stickers'), ('textbook'), ('mathematics'), ('airpods'), ('earbuds'),
    ('wallet'), ('leather'), ('jacket'), ('denim');

-- Item-tag associations
INSERT INTO item_tags (item_id, tag_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),   -- iPhone: phone, apple, black, cracked screen, lanyard
    (2, 1), (2, 2), (2, 3), (2, 4), (2, 5),   -- Found iPhone: same tags
    (3, 6), (3, 7), (3, 8), (3, 5),            -- Keys: keys, red, padlock, lanyard
    (4, 6), (4, 7), (4, 8),                     -- Found Keys: keys, red, padlock
    (5, 9), (5, 10), (5, 11), (5, 12),          -- Backpack: backpack, laptop, hp, blue
    (6, 9), (6, 10), (6, 12),                   -- Found Backpack: backpack, laptop, blue
    (7, 13), (7, 14),                            -- Student ID: id card, student
    (8, 15), (8, 16), (8, 12),                  -- Water Bottle: water bottle, stickers, blue
    (9, 17), (9, 18),                            -- Textbook: textbook, mathematics
    (10, 19), (10, 20), (10, 2),                -- AirPods: airpods, earbuds, apple
    (11, 21), (11, 22),                          -- Wallet: wallet, leather
    (12, 23), (12, 24), (12, 3);                -- Jacket: jacket, denim, black

-- ============================================================
-- Matches (auto-detected pairs)
-- ============================================================

-- iPhone match (items 1 & 2) — high confidence
INSERT INTO matches (lost_item_id, found_item_id, score, ai_explanation, status) VALUES
    (1, 2, 0.92,
     'Both items are black iPhones found in Balme Library on the same day. The lost phone has a cracked screen protector and red lanyard — the found phone matches both details exactly. The location is within 30 meters, and both are categorized as Electronics with the Apple brand.',
     'SUGGESTED');

-- Keys match (items 3 & 4) — good confidence
INSERT INTO matches (lost_item_id, found_item_id, score, ai_explanation, status) VALUES
    (3, 4, 0.78,
     'Both are sets of keys on a red lanyard reported on the same day. The lost set has 4 keys including a padlock key, and the found set has 4-5 keys with what appears to be a padlock key. They were found at nearby campus locations (Bush Canteen and Great Hall are about 400m apart).',
     'SUGGESTED');

-- Backpack match (items 5 & 6) — high confidence
INSERT INTO matches (lost_item_id, found_item_id, score, ai_explanation, status) VALUES
    (5, 6, 0.88,
     'Both are blue laptop backpacks left in JQB on the same day. The distinguishing detail is the tear/rip on the front pocket mentioned in both reports. The lost bag contains an HP laptop and notebooks — the found bag also has a laptop and notebooks inside. Location is nearly identical.',
     'SUGGESTED');

-- ============================================================
-- Claims
-- ============================================================

-- Kwame claims the found iPhone (item 2) on behalf of Ama
INSERT INTO claims (item_id, claimant_id, status, message) VALUES
    (2, 2, 'PENDING',
     'This is my iPhone! I lost it in Balme Library on August 5th. It has a cracked screen protector and my red UG lanyard. I can show you my Apple ID login as proof of ownership.');

-- ============================================================
-- Conversations & Messages
-- ============================================================

-- Conversation between Ama (claimant) and Kwame (finder of iPhone)
INSERT INTO conversations (item_id, user_a_id, user_b_id, last_message_at) VALUES
    (2, 3, 2, '2026-08-09 14:30:00');

INSERT INTO messages (conversation_id, sender_id, body, is_read, created_at) VALUES
    (1, 2, 'Hi! I saw you found an iPhone in Balme. I think it might be mine — I lost a black iPhone 14 Pro with a cracked screen protector there on the 5th.', true, '2026-08-09 10:15:00'),
    (1, 3, 'Hey Ama! Yes, I found it on a desk on the second floor. Can you describe the case and any other identifying features?', true, '2026-08-09 10:22:00'),
    (1, 2, 'It has a plain black silicone case and a red UG lanyard attached to it. The wallpaper is a photo of Cape Coast Castle. Also there should be a tiny scratch on the top right corner.', true, '2026-08-09 10:30:00'),
    (1, 3, 'That matches perfectly! I have it at the Balme Library front desk. You can pick it up anytime — just tell them Kwame Asante left it there.', true, '2026-08-09 11:00:00'),
    (1, 2, 'Thank you so much! I will come by this afternoon. You are a lifesaver!', false, '2026-08-09 14:30:00');

-- ============================================================
-- Notifications
-- ============================================================
INSERT INTO notifications (user_id, type, title, body, link, is_read, created_at) VALUES
    (2, 'MATCH', 'Match found!', 'A possible match was found for your lost iPhone 14 Pro', '/items/1/matches', true, '2026-08-05 16:00:00'),
    (3, 'MATCH', 'Match found!', 'A possible match was found for your found iPhone', '/items/2/matches', true, '2026-08-05 16:00:00'),
    (3, 'CLAIM', 'New claim on your item', 'Ama Mensah claims "Black iPhone in Silicone Case"', '/items/2', false, '2026-08-09 10:10:00'),
    (4, 'MATCH', 'Match found!', 'A possible match was found for your lost keys', '/items/3/matches', false, '2026-08-07 18:00:00'),
    (5, 'MATCH', 'Match found!', 'A possible match was found for your found keys', '/items/4/matches', false, '2026-08-07 18:00:00'),
    (3, 'MATCH', 'Match found!', 'A possible match was found for your lost backpack', '/items/5/matches', false, '2026-08-08 15:00:00'),
    (2, 'MATCH', 'Match found!', 'A possible match was found for your found backpack', '/items/6/matches', false, '2026-08-08 15:00:00'),
    (2, 'MESSAGE', 'New message', 'You have a new message about the iPhone', '/messages/1', false, '2026-08-09 10:22:00');


-- ------------------------------------------------------------
-- V3__fix_seed_passwords.sql
-- ------------------------------------------------------------
-- V3 — Fix seed user passwords (BCrypt hash of "Password123")
UPDATE users
SET password_hash = '$2a$10$IOFQDV0tgCz2i68Xr4RE1uJiuG0w4hRvqDSEBbyTBDk4i.3DkWcLK'
WHERE email IN (
    'admin@reclaim.app',
    'ama.mensah@ug.edu.gh',
    'kwame.asante@ug.edu.gh',
    'efua.darkwa@ug.edu.gh',
    'kofi.boateng@ug.edu.gh'
);


-- ------------------------------------------------------------
-- V4__fix_seed_data.sql
-- ------------------------------------------------------------
-- V4 — Fix seed data issues reported during testing
-- 1. Replace broken Unsplash image URLs for items 3 and 4 (keys)
-- 2. Fix item 6 color from "Blue" to "Brown" (matches the actual bag description better)

-- Fix broken key images with reliable Unsplash photos
UPDATE item_photos SET url = 'https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=600&h=400&fit=crop'
WHERE item_id = 3;

UPDATE item_photos SET url = 'https://images.unsplash.com/photo-1609151354296-e82e1e3e7c1d?w=600&h=400&fit=crop'
WHERE item_id = 4;

-- Fix item 6: color should be Brown, not Blue (user reported this)
UPDATE items SET color = 'Brown' WHERE id = 6;


-- ------------------------------------------------------------
-- V5__delete_broken_items_fix_title.sql
-- ------------------------------------------------------------
-- V5 — Delete items with broken Unsplash images + fix item 6 title
-- Items 3 (Bunch of Keys with Red Lanyard) and 4 (Set of Keys on Lanyard) have
-- broken image URLs. User requested deletion.
-- Item 6 title still says "Blue" but color was corrected to "Brown" in V4.

-- Delete dependent rows first (order matters for FK constraints)
DELETE FROM messages WHERE conversation_id IN (SELECT id FROM conversations WHERE item_id IN (3, 4));
DELETE FROM conversations WHERE item_id IN (3, 4);
DELETE FROM notifications WHERE link LIKE '/items/3%' OR link LIKE '/items/4%';
DELETE FROM claims WHERE item_id IN (3, 4);
DELETE FROM item_tags WHERE item_id IN (3, 4);
DELETE FROM item_photos WHERE item_id IN (3, 4);
DELETE FROM matches WHERE lost_item_id IN (3, 4) OR found_item_id IN (3, 4);
DELETE FROM items WHERE id IN (3, 4);

-- Fix item 6 title: "Blue Laptop Bag" → "Brown Laptop Bag"
UPDATE items SET title = 'Brown Laptop Bag Found in JQB' WHERE id = 6;


-- ------------------------------------------------------------
-- V6__delete_test_item.sql
-- ------------------------------------------------------------
-- V6 — Delete test item 26 (pgAdmin screenshot with no photo, created during testing)
DELETE FROM messages WHERE conversation_id IN (SELECT id FROM conversations WHERE item_id = 26);
DELETE FROM conversations WHERE item_id = 26;
DELETE FROM notifications WHERE link LIKE '/items/26%';
DELETE FROM claims WHERE item_id = 26;
DELETE FROM item_tags WHERE item_id = 26;
DELETE FROM item_photos WHERE item_id = 26;
DELETE FROM matches WHERE lost_item_id = 26 OR found_item_id = 26;
DELETE FROM items WHERE id = 26;


-- ------------------------------------------------------------
-- V7__fix_item2_broken_image.sql
-- ------------------------------------------------------------
-- V7 — Fix broken Unsplash image on item 2 (Black iPhone in Silicone Case)
-- The old URL (photo-1695048065319) returns 404. Replace with a working iPhone image.
UPDATE item_photos
SET url = 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&h=400&fit=crop'
WHERE item_id = 2;


-- ------------------------------------------------------------
-- V8__remove_ws_test_messages.sql
-- ------------------------------------------------------------
-- V8 — Remove test messages/notifications created while verifying live messaging.
-- These were injected during WebSocket/polling end-to-end tests and would
-- otherwise clutter the seeded demo conversation.

DELETE FROM notifications
WHERE type = 'MESSAGE'
  AND (body LIKE 'WARM-%'
    OR body LIKE 'UISOCK-%'
    OR body LIKE 'UI-SOCKET-%'
    OR body LIKE 'WS-PUSH-%'
    OR body LIKE 'POLL-TEST-%'
    OR body LIKE 'LIVE-%'
    OR body LIKE 'Unread test message from Kwame%');

DELETE FROM messages
WHERE body LIKE 'WARM-%'
   OR body LIKE 'UISOCK-%'
   OR body LIKE 'UI-SOCKET-%'
   OR body LIKE 'WS-PUSH-%'
   OR body LIKE 'POLL-TEST-%'
   OR body LIKE 'LIVE-%'
   OR body LIKE 'Unread test message from Kwame%';

