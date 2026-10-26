package com.bogusz.bicyclerouteplannerbackend.dto.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameRouteRequestDto(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name cannot exceed 150 characters")
        String name
) {
}
