package com.reclaim.dto.response;

import com.reclaim.entity.Claim;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class ClaimResponse {
    private Long id;
    private Long itemId;
    private String itemTitle;
    private UserResponse claimant;
    private String status;
    private String message;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public static ClaimResponse from(Claim c) {
        return ClaimResponse.builder()
            .id(c.getId())
            .itemId(c.getItem().getId())
            .itemTitle(c.getItem().getTitle())
            .claimant(UserResponse.from(c.getClaimant()))
            .status(c.getStatus())
            .message(c.getMessage())
            .reviewedAt(c.getReviewedAt())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
