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
