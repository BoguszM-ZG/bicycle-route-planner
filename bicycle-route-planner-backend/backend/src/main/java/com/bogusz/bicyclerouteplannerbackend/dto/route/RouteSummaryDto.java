package com.bogusz.bicyclerouteplannerbackend.dto.route;

public record RouteSummaryDto(
        Long id,
        String name,
        Double distanceMeters
) {
}
