package com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvoidanceZoneDto {

    private Long id;
    private String name;
    private String zoneGeom;
    private Boolean isActive;

}
