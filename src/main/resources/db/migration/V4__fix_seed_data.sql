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
