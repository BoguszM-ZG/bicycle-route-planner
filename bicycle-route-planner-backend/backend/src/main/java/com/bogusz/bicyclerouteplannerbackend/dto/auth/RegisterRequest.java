package com.bogusz.bicyclerouteplannerbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters long")
        String password,

        @Size(max = 255, message = "Full name cannot exceed 255 characters")
        @NotBlank(message = "Full name is required")
        String fullName
) {
}