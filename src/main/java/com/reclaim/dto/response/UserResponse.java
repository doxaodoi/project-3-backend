package com.reclaim.dto.response;

import com.reclaim.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String avatarUrl;
    private LocalDateTime createdAt;

    public static UserResponse from(User u) {
        return UserResponse.builder()
            .id(u.getId())
            .fullName(u.getFullName())
            .email(u.getEmail())
            .phone(u.getPhone())
            .role(u.getRole())
            .avatarUrl(u.getAvatarUrl())
            .createdAt(u.getCreatedAt())
            .build();
    }
}
