package com.bogusz.bicyclerouteplannerbackend.dto.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import java.time.Instant;

import com.bogusz.bicyclerouteplannerbackend.model.RouteType;
import jakarta.validation.constraints.NotNull;

@Builder
public record RouteDetailDto(
        Long id,

        @NotBlank(message = "Name of the route is required")
        String name,

        @NotNull(message = "Type of the route is required")
        RouteType routeType,

        @PositiveOrZero(message = "Distance must be positive")
        Double targetDistance,

        @PositiveOrZero(message = "Distance must be positive")
        Double distanceMeters,

        @PositiveOrZero
        Double elevationGain,

        @PositiveOrZero
        Double elevationLoss,

        @NotBlank(message = "Start point is required")
        String startPoint,

        @NotBlank(message = "Route geometry is required")
        String routeGeom,

        Instant createdAt,

        Long folderId
) {}