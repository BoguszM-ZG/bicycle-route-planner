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
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public Route toEntity(CreateRouteRequestDto dto, User user, Folder folder) {
        Route route = new Route();
        route.setUser(user);
        route.setName(dto.name());
        route.setRouteType(dto.routeType());
        route.setTargetDistance(dto.targetDistance());
        route.setDistanceMeters(dto.distanceMeters());
        route.setElevationGain(dto.elevationGain());
        route.setElevationLoss(dto.elevationLoss());
        route.setFolder(folder);

        route.setStartPoint(parsePoint(dto.startPoint()));
        route.setRouteGeom(parseLineString(dto.routeGeom()));

        return route;
    }

    public RouteDetailDto toRouteDetailDto(Route route) {
        WKTWriter writer = new WKTWriter(3);
        return RouteDetailDto.builder()
                .id(route.getId())
                .name(route.getName())
                .routeType(route.getRouteType())
                .targetDistance(route.getTargetDistance())
                .distanceMeters(route.getDistanceMeters())
                .elevationGain(route.getElevationGain())
                .elevationLoss(route.getElevationLoss())
                .startPoint(route.getStartPoint() != null ? writer.write(route.getStartPoint()) : null)
                .routeGeom(route.getRouteGeom() != null ? writer.write(route.getRouteGeom()) : null)
                .createdAt(route.getCreatedAt())
                .folderId(route.getFolder() != null ? route.getFolder().getId() : null)
                .build();
    }

    private Point parsePoint(String wkt) {
        try {
            Geometry geom = new WKTReader().read(wkt);
            geom.setSRID(4326);
            return (Point) geom;
        } catch (Exception e) {
            throw new InvalidGeometryException("Failed to parse start point geometry", e);
        }
    }

    private LineString parseLineString(String wkt) {
        try {
            Geometry geom = new WKTReader().read(wkt);
            geom.setSRID(4326);
            return (LineString) geom;
        } catch (Exception e) {
            throw new InvalidGeometryException("Failed to parse route line geometry", e);
        }
    }
}
