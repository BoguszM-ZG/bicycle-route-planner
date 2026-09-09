package com.bogusz.bicyclerouteplannerbackend.dto.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateRouteRequestDto(
        @NotBlank(message = "Name of the route is required")
        String name,

        @NotBlank(message = "Type of the route is required")
        String routeType,

        @PositiveOrZero(message = "Distance must be positive")
        Double targetDistance,

        @PositiveOrZero(message = "Distance must be positive")
        Double distanceMeters,

        @PositiveOrZero(message = "Elevation gain must be positive")
        Double elevationGain,

        @PositiveOrZero(message = "Elevation loss must be positive")
        Double elevationLoss,

        @NotBlank(message = "Start point is required")
        String startPoint,

        @NotBlank(message = "Route geometry is required")
        String routeGeom,

        Long folderId
) {
}
