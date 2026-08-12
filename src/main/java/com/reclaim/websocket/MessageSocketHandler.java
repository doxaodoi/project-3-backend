package com.reclaim.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reclaim.config.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw WebSocket endpoint for live messaging.
 *
 * Auth uses an auth-on-first-message pattern (so the JWT never rides in the
 * handshake URL): a freshly connected socket is unauthenticated until it sends
 * {"type":"auth","token":"<jwt>"}. Once validated, the session is tracked by
 * userId. The server pushes messages only to the participants of a
 * conversation — clients cannot subscribe to arbitrary channels, so there is no
 * way to eavesdrop on someone else's thread.
 */
@Component
public class MessageSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID = "userId";

    private final JwtUtil jwtUtil;
    private final ObjectMapper mapper;

    /** userId -> that user's live sessions (may be several open tabs/devices). */
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public MessageSocketHandler(JwtUtil jwtUtil, ObjectMapper mapper) {
        this.jwtUtil = jwtUtil;
        this.mapper = mapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node;
        try {
            node = mapper.readTree(message.getPayload());
        } catch (Exception e) {
            return; // ignore malformed frames
        }

        String type = node.path("type").asText("");

        if ("auth".equals(type)) {
            String token = node.path("token").asText("");
            if (token.isBlank() || !jwtUtil.isValid(token)) {
                safeSend(session, "{\"type\":\"auth-error\"}");
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }
            Long userId = jwtUtil.getUserId(token);
            session.getAttributes().put(USER_ID, userId);
            sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
            safeSend(session, "{\"type\":\"auth-ok\"}");
        } else if ("ping".equals(type)) {
            safeSend(session, "{\"type\":\"pong\"}");
        }
        // Clients never send message content over the socket — messages are
        // created through the authenticated REST endpoint, then pushed here.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object uid = session.getAttributes().get(USER_ID);
        if (uid instanceof Long userId) {
            Set<WebSocketSession> set = sessions.get(userId);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) sessions.remove(userId);
            }
        }
    }

    /** Push a raw JSON payload to every live session of the given user. */
    public void sendToUser(Long userId, String json) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null) return;
        TextMessage frame = new TextMessage(json);
        for (WebSocketSession s : set) {
            if (s.isOpen()) safeSend(s, frame);
        }
    }

    private void safeSend(WebSocketSession session, String json) {
        safeSend(session, new TextMessage(json));
    }

    private void safeSend(WebSocketSession session, TextMessage frame) {
        try {
            synchronized (session) {
                session.sendMessage(frame);
            }
        } catch (IOException ignored) {
            // session likely closing; afterConnectionClosed will clean it up
        }
    }
}
