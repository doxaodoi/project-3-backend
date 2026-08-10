package com.reclaim.dto.response;

import com.reclaim.entity.ItemPhoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @Builder @AllArgsConstructor
public class PhotoResponse {
    private Long id;
    private String url;
    private Boolean isPrimary;

    public static PhotoResponse from(ItemPhoto p) {
        return PhotoResponse.builder()
            .id(p.getId())
            .url(p.getUrl())
            .isPrimary(p.getIsPrimary())
            .build();
    }
}
