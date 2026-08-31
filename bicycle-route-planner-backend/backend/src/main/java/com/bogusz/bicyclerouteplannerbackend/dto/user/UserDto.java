package com.bogusz.bicyclerouteplannerbackend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import java.time.Instant;

@Builder
public record UserDto(
        Long id,

        @Email(message = "Invalid email format")
        @NotBlank
        String email,

        @NotBlank
        String fullName,

        String role,
        Boolean active,
        Instant createdAt
) {}