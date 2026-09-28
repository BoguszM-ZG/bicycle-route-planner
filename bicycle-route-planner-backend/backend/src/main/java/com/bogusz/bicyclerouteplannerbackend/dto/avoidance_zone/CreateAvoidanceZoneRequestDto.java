package com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAvoidanceZoneRequestDto(
        @NotBlank(message = "Name of the avoidance zone is required")
        @Size(max = 100, message = "Name of the avoidance zone cannot exceed 100 characters")
        String name,

        @NotBlank(message = "Geometry of the avoidance zone is required")
        String zoneGeom
) {
}
