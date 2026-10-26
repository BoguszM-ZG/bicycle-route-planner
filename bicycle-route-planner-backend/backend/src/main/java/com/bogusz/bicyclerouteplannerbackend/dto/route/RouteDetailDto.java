package com.bogusz.bicyclerouteplannerbackend.dto.route;

import com.bogusz.bicyclerouteplannerbackend.model.RouteType;
import lombok.Builder;
import java.time.Instant;

@Builder
public record RouteDetailDto(
        Long id,
        String name,
        RouteType routeType,
        Double targetDistance,
        Double distanceMeters,
        Double elevationGain,
        Double elevationLoss,
        String startPoint,
        String routeGeom,
        Instant createdAt,
        Long folderId
) {}