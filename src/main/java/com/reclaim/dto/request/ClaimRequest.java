package com.reclaim.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimRequest {
    @NotBlank
    private String message;
}
