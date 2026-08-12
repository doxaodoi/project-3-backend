package com.reclaim.service;

import com.reclaim.dto.request.MessageRequest;
import com.reclaim.dto.response.ConversationResponse;
import com.reclaim.dto.response.MessageResponse;
import com.reclaim.entity.Conversation;
import com.reclaim.entity.Message;
import com.reclaim.entity.Notification;
import com.reclaim.entity.User;
import com.reclaim.exception.ApiException;
import com.reclaim.repository.ConversationRepository;
import com.reclaim.repository.MessageRepository;
import com.reclaim.repository.NotificationRepository;
import com.reclaim.websocket.NewMessageEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final NotificationRepository notifRepo;
    private final ApplicationEventPublisher events;

    public ConversationService(ConversationRepository convRepo,
                               MessageRepository msgRepo,
                               NotificationRepository notifRepo,
                               ApplicationEventPublisher events) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.notifRepo = notifRepo;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(User user) {
        return convRepo.findByUserId(user.getId()).stream()
            .map(c -> {
                long unread = c.getMessages().stream()
                    .filter(m -> !m.getSender().getId().equals(user.getId()) && !m.getIsRead())
                    .count();
                return ConversationResponse.from(c, user.getId(), unread);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, User user) {
        Conversation conv = convRepo.findById(conversationId)
            .orElseThrow(() -> ApiException.notFound("Conversation"));

        if (!conv.getUserA().getId().equals(user.getId())
                && !conv.getUserB().getId().equals(user.getId())) {
            throw ApiException.forbidden("Not a participant in this conversation");
        }

        return msgRepo.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
            .map(MessageResponse::from)
            .toList();
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, MessageRequest req, User sender) {
        Conversation conv = convRepo.findById(conversationId)
            .orElseThrow(() -> ApiException.notFound("Conversation"));

        if (!conv.getUserA().getId().equals(sender.getId())
                && !conv.getUserB().getId().equals(sender.getId())) {
            throw ApiException.forbidden("Not a participant in this conversation");
        }

        Message msg = Message.builder()
            .conversation(conv)
            .sender(sender)
            .body(req.getBody())
            .build();
        msgRepo.save(msg);

        conv.setLastMessageAt(LocalDateTime.now());
        convRepo.save(conv);

        // Notify the other user
        User recipient = conv.getUserA().getId().equals(sender.getId())
            ? conv.getUserB() : conv.getUserA();

        notifRepo.save(Notification.builder()
            .user(recipient)
            .type("MESSAGE")
            .title("New message from " + sender.getFullName())
            .body(req.getBody().length() > 80 ? req.getBody().substring(0, 80) + "..." : req.getBody())
            .link("/messages/" + conversationId)
            .build());

        MessageResponse dto = MessageResponse.from(msg);

        // Push to both participants over WebSocket, after this transaction commits.
        events.publishEvent(new NewMessageEvent(
            conversationId,
            List.of(sender.getId(), recipient.getId()),
            dto
        ));

        return dto;
    }

    @Transactional
    public void markRead(Long messageId, User user) {
        Message msg = msgRepo.findById(messageId)
            .orElseThrow(() -> ApiException.notFound("Message"));
        // Only the recipient can mark as read
        if (!msg.getSender().getId().equals(user.getId())) {
            msg.setIsRead(true);
            msgRepo.save(msg);
        }
    }

    /**
     * Mark a whole conversation read for the given user: every incoming
     * message and any message notifications that point at it. Called when the
     * user opens the thread, so the unread dot and bell both clear.
     */
    @Transactional
    public void markConversationRead(Long conversationId, User user) {
        Conversation conv = convRepo.findById(conversationId)
            .orElseThrow(() -> ApiException.notFound("Conversation"));

        if (!conv.getUserA().getId().equals(user.getId())
                && !conv.getUserB().getId().equals(user.getId())) {
            throw ApiException.forbidden("Not a participant in this conversation");
        }

        msgRepo.markConversationRead(conversationId, user.getId());
        notifRepo.markReadByLink(user.getId(), "/messages/" + conversationId);
    }
}
