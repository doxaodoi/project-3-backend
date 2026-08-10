package com.reclaim.dto.response;

import com.reclaim.entity.Match;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class MatchResponse {
    private Long id;
    private ItemResponse lostItem;
    private ItemResponse foundItem;
    private Double score;
    private String aiExplanation;
    private String status;
    private LocalDateTime createdAt;

    public static MatchResponse from(Match m) {
        return MatchResponse.builder()
            .id(m.getId())
            .lostItem(ItemResponse.from(m.getLostItem()))
            .foundItem(ItemResponse.from(m.getFoundItem()))
            .score(m.getScore())
            .aiExplanation(m.getAiExplanation())
            .status(m.getStatus())
            .createdAt(m.getCreatedAt())
            .build();
    }
}
