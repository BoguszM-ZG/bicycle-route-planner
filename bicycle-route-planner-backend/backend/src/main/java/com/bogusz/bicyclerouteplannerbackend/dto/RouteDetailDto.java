package com.bogusz.bicyclerouteplannerbackend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class RouteDetailDto {
    Long id;
    String userEmail;
    String name;
    String routeType;
    Double targetDistance;
    Double distanceMeters;
    Double elevationGain;
    Double elevationLoss;
    String startPoint;
    String routeGeom;
    Instant createdAt;
}
