package com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AvoidanceZoneDto(
        Long id,

        @NotBlank(message = "Name of the avoidance zone is required")
        String name,

        @NotBlank(message = "Geometry of the avoidance zone is required")
        String zoneGeom,

        @NotNull(message = "Availability of the avoidance zone is required")
        Boolean isActive
) {}