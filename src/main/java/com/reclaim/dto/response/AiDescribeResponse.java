package com.reclaim.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder @AllArgsConstructor
public class AiDescribeResponse {
    private String title;
    private String description;
    private String category;
    private String color;
    private String brand;
    private List<String> tags;
}
