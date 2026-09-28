package com.bogusz.bicyclerouteplannerbackend.dto.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import com.bogusz.bicyclerouteplannerbackend.model.RouteType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRouteRequestDto(
        @NotBlank(message = "Name of the route is required")
        @Size(max = 150, message = "Name of the route cannot exceed 150 characters")
        String name,

        @NotNull(message = "Type of the route is required")
        RouteType routeType,

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
