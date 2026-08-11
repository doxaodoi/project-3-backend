-- V7 — Fix broken Unsplash image on item 2 (Black iPhone in Silicone Case)
-- The old URL (photo-1695048065319) returns 404. Replace with a working iPhone image.
UPDATE item_photos
SET url = 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&h=400&fit=crop'
WHERE item_id = 2;
