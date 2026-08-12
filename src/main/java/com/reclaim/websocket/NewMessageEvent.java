package com.reclaim.websocket;

import com.reclaim.dto.response.MessageResponse;

import java.util.List;

/**
 * Published when a message is persisted. Carries the conversation id, the
 * participant user ids to deliver to, and the message payload. A broadcaster
 * fans this out over WebSocket after the transaction commits.
 */
public record NewMessageEvent(
        Long conversationId,
        List<Long> participantIds,
        MessageResponse message
) {}
