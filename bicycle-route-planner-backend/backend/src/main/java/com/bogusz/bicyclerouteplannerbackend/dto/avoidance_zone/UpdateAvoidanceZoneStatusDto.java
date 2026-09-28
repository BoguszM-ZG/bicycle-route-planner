package com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone;

import jakarta.validation.constraints.NotNull;

public record UpdateAvoidanceZoneStatusDto(
        @NotNull(message = "Active status is required")
        Boolean isActive
) {
}
