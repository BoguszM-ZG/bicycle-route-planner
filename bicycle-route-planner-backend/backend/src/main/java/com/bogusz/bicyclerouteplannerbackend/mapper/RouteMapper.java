package com.bogusz.bicyclerouteplannerbackend.mapper;

import com.bogusz.bicyclerouteplannerbackend.dto.route.CreateRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.exception.InvalidGeometryException;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public Route toEntity(CreateRouteRequestDto dto, User user, Folder folder) {
        WKTReader wktReader = new WKTReader();
        Route route = new Route();

        route.setUser(user);
        route.setName(dto.name());
        route.setRouteType(dto.routeType());
        route.setTargetDistance(dto.targetDistance());
        route.setDistanceMeters(dto.distanceMeters());
        route.setElevationGain(dto.elevationGain());
        route.setElevationLoss(dto.elevationLoss());
        route.setFolder(folder);

        try {
            if (dto.startPoint() != null) {
                Geometry startGeom = wktReader.read(dto.startPoint());
                startGeom.setSRID(4326);
                route.setStartPoint((Point) startGeom);
            }

            if (dto.routeGeom() != null) {
                Geometry routeGeom = wktReader.read(dto.routeGeom());
                routeGeom.setSRID(4326);
                route.setRouteGeom((LineString) routeGeom);
            }
        } catch (ParseException e) {
            throw new InvalidGeometryException("Error parsing WKT geometry: " + e.getMessage());
        }

        return route;
    }

    public RouteDetailDto toRouteDetailDto(Route route) {
        WKTWriter wktWriter3D = new WKTWriter(3);

        return RouteDetailDto.builder()
                .id(route.getId())
                .name(route.getName())
                .routeType(route.getRouteType())
                .targetDistance(route.getTargetDistance())
                .distanceMeters(route.getDistanceMeters())
                .elevationGain(route.getElevationGain())
                .elevationLoss(route.getElevationLoss())
                .startPoint(route.getStartPoint() != null ? wktWriter3D.write(route.getStartPoint()) : null)
                .routeGeom(route.getRouteGeom() != null ? wktWriter3D.write(route.getRouteGeom()) : null)
                .createdAt(route.getCreatedAt())
                .folderId(route.getFolder() != null ? route.getFolder().getId() : null)
                .build();
    }
}
