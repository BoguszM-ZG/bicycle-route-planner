package com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone;

import lombok.Builder;

@Builder
public record AvoidanceZoneDto(
        Long id,
        String name,
        String zoneGeom,
        Boolean isActive
) {}