package com.reclaim.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fans a persisted message out to its participants over WebSocket, but only
 * AFTER the surrounding DB transaction commits — so we never push a message
 * that later rolls back.
 */
@Component
public class MessageSocketBroadcaster {

    private final MessageSocketHandler handler;
    private final ObjectMapper mapper;

    public MessageSocketBroadcaster(MessageSocketHandler handler, ObjectMapper mapper) {
        this.handler = handler;
        this.mapper = mapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewMessage(NewMessageEvent event) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "message");
        envelope.put("conversationId", event.conversationId());
        envelope.put("message", event.message());

        String json;
        try {
            json = mapper.writeValueAsString(envelope);
        } catch (Exception e) {
            return; // serialization shouldn't fail; if it does, clients still poll
        }

        for (Long userId : event.participantIds()) {
            handler.sendToUser(userId, json);
        }
    }
}
