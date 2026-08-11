-- V6 — Delete test item 26 (pgAdmin screenshot with no photo, created during testing)
DELETE FROM messages WHERE conversation_id IN (SELECT id FROM conversations WHERE item_id = 26);
DELETE FROM conversations WHERE item_id = 26;
DELETE FROM notifications WHERE link LIKE '/items/26%';
DELETE FROM claims WHERE item_id = 26;
DELETE FROM item_tags WHERE item_id = 26;
DELETE FROM item_photos WHERE item_id = 26;
DELETE FROM matches WHERE lost_item_id = 26 OR found_item_id = 26;
DELETE FROM items WHERE id = 26;
