package com.reclaim.dto.response;

import com.reclaim.entity.Conversation;
import com.reclaim.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private Long itemId;
    private String itemTitle;
    private UserResponse otherUser;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public static ConversationResponse from(Conversation c, Long currentUserId, long unreadCount) {
        // Show the other participant
        boolean isUserA = c.getUserA().getId().equals(currentUserId);
        var otherUser = isUserA ? c.getUserB() : c.getUserA();

        String preview = null;
        if (!c.getMessages().isEmpty()) {
            Message last = c.getMessages().get(c.getMessages().size() - 1);
            preview = last.getBody().length() > 80
                ? last.getBody().substring(0, 80) + "..."
                : last.getBody();
        }

        return ConversationResponse.builder()
            .id(c.getId())
            .itemId(c.getItem() != null ? c.getItem().getId() : null)
            .itemTitle(c.getItem() != null ? c.getItem().getTitle() : null)
            .otherUser(UserResponse.from(otherUser))
            .lastMessagePreview(preview)
            .lastMessageAt(c.getLastMessageAt())
            .unreadCount(unreadCount)
            .build();
    }
}
