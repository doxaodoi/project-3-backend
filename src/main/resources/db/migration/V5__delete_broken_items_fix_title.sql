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
