package com.reclaim.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(max = 120)
    private String fullName;

    @NotBlank @Email
    private String email;

    private String phone;

    @NotBlank @Size(min = 6, max = 100)
    private String password;
}
