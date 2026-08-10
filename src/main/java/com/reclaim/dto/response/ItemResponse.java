package com.reclaim.dto.response;

import com.reclaim.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data @Builder @AllArgsConstructor
public class ItemResponse {
    private Long id;
    private String type;
    private String title;
    private String description;
    private String category;
    private Long categoryId;
    private String location;
    private Long locationId;
    private String heldAt;
    private Double latitude;
    private Double longitude;
    private LocalDate eventDate;
    private String status;
    private String color;
    private String brand;
    private Boolean isAiDescribed;
    private List<String> tags;
    private List<PhotoResponse> photos;
    private UserResponse reporter;
    private int matchCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ItemResponse from(Item item) {
        return from(item, 0);
    }

    public static ItemResponse from(Item item, int matchCount) {
        return ItemResponse.builder()
            .id(item.getId())
            .type(item.getType())
            .title(item.getTitle())
            .description(item.getDescription())
            .category(item.getCategory() != null ? item.getCategory().getName() : null)
            .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
            .location(item.getLocation() != null ? item.getLocation().getName() : null)
            .locationId(item.getLocation() != null ? item.getLocation().getId() : null)
            .heldAt(item.getHeldAt())
            .latitude(item.getLatitude())
            .longitude(item.getLongitude())
            .eventDate(item.getEventDate())
            .status(item.getStatus())
            .color(item.getColor())
            .brand(item.getBrand())
            .isAiDescribed(item.getIsAiDescribed())
            .tags(item.getTags().stream().map(t -> t.getName()).collect(Collectors.toList()))
            .photos(item.getPhotos().stream().map(PhotoResponse::from).collect(Collectors.toList()))
            .reporter(UserResponse.from(item.getReporter()))
            .matchCount(matchCount)
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
