package com.reclaim.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ItemRequest {
    @NotBlank
    private String type; // LOST or FOUND

    @NotBlank @Size(max = 200)
    private String title;

    private String description;
    private Long categoryId;
    private Long locationId;
    private String locationName; // free-text location (geocoded on frontend)
    private String heldAt;
    private Double latitude;
    private Double longitude;
    private LocalDate eventDate;
    private String color;
    private String brand;
    private Boolean isAiDescribed;
    private List<String> tags;
    private List<String> photoUrls;
}
