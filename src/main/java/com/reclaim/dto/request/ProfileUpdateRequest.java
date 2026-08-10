package com.reclaim.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 120)
    private String fullName;

    private String phone;
    private String avatarUrl;
}
