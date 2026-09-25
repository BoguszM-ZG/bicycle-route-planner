package com.bogusz.bicyclerouteplannerbackend.mapper;

import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.CreateAvoidanceZoneRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.InvalidGeometryException;
import com.bogusz.bicyclerouteplannerbackend.model.AvoidanceZone;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;

@Component
public class AvoidanceZoneMapper {

    public AvoidanceZone toEntity(CreateAvoidanceZoneRequestDto request, User user) {
        WKTReader wktReader = new WKTReader();
        AvoidanceZone avoidanceZone = new AvoidanceZone();
        avoidanceZone.setUser(user);
        avoidanceZone.setName(request.name());
        try {
            if (request.zoneGeom() != null) {
                Geometry geom = wktReader.read(request.zoneGeom());
                geom.setSRID(4326);
                avoidanceZone.setZoneGeom((Polygon) geom);
            }
        } catch (ParseException e) {
            throw new InvalidGeometryException("Error parsing WKT geometry " + e.getMessage());
        }
        avoidanceZone.setIsActive(true);
        return avoidanceZone;
    }

    public AvoidanceZoneDto toDto(AvoidanceZone avoidanceZone) {
        WKTWriter wktWriter = new WKTWriter();
        return AvoidanceZoneDto.builder()
                .id(avoidanceZone.getId())
                .name(avoidanceZone.getName())
                .zoneGeom(avoidanceZone.getZoneGeom() != null ? wktWriter.write(avoidanceZone.getZoneGeom()) : null)
                .isActive(avoidanceZone.getIsActive())
                .build();
    }
}
