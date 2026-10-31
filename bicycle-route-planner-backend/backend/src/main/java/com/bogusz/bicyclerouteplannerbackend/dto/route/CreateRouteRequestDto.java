package com.bogusz.bicyclerouteplannerbackend.dto.route;

import com.bogusz.bicyclerouteplannerbackend.model.RouteType;
import com.bogusz.bicyclerouteplannerbackend.validation.GeometryType;
import com.bogusz.bicyclerouteplannerbackend.validation.ValidWktGeometry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
        @ValidWktGeometry(expectedType = GeometryType.POINT)
        String startPoint,

        @NotBlank(message = "Route geometry is required")
        @ValidWktGeometry(expectedType = GeometryType.LINE_STRING)
        String routeGeom,

        Long folderId
) {
}
