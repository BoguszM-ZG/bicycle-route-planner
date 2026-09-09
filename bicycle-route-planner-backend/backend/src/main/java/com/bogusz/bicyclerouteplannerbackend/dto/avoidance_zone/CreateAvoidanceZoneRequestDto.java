package com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone;

import jakarta.validation.constraints.NotBlank;

public record CreateAvoidanceZoneRequestDto(
        @NotBlank(message = "Name of the avoidance zone is required")
        String name,

        @NotBlank(message = "Geometry of the avoidance zone is required")
        String zoneGeom
) {
}
