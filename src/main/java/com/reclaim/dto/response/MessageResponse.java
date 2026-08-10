package com.reclaim.dto.response;

import com.reclaim.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String body;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static MessageResponse from(Message m) {
        return MessageResponse.builder()
            .id(m.getId())
            .senderId(m.getSender().getId())
            .senderName(m.getSender().getFullName())
            .body(m.getBody())
            .isRead(m.getIsRead())
            .createdAt(m.getCreatedAt())
            .build();
    }
}
