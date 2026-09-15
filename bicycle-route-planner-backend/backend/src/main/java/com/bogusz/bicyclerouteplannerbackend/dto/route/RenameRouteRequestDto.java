package com.bogusz.bicyclerouteplannerbackend.dto.route;

import jakarta.validation.constraints.NotBlank;

public record RenameRouteRequestDto(
        @NotBlank(message = "Name is required")
        String name
) {
}
