package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final UserService userService;


    public List<RouteDetailDto> getAllRoutes(){
        return routeRepository.findAll().stream()
                .map(this::toRouteDetailDto)
                .collect(Collectors.toList());
    }

    public RouteDetailDto saveRoute(RouteDetailDto routeDetailDto) {
        Route route = toRoute(routeDetailDto);
        route = routeRepository.save(route);
        return toRouteDetailDto(route);
    }

    private RouteDetailDto toRouteDetailDto(Route route) {
        WKTWriter wtkWriter3D = new WKTWriter(3);

        return RouteDetailDto.builder()
                .id(route.getId())
                .userEmail(route.getUser() != null ? route.getUser().getEmail() : null)
                .name(route.getName())
                .routeType(route.getRouteType())
                .targetDistance(route.getTargetDistance())
                .distanceMeters(route.getDistanceMeters())
                .elevationGain(route.getElevationGain())
                .elevationLoss(route.getElevationLoss())
                .startPoint(route.getStartPoint() != null ? wtkWriter3D.write(route.getStartPoint()) : null)
                .routeGeom(route.getRouteGeom() != null ? wtkWriter3D.write(route.getRouteGeom()) : null)
                .createdAt(route.getCreatedAt())
                .build();
    }

    private Route toRoute(RouteDetailDto routeDetailDto) {
        WKTReader wktReader = new WKTReader();
        Route route = new Route();

        String userEmail = routeDetailDto.getUserEmail();
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail is required");
        }

        route.setUser(userRepository.findByEmail(userEmail)
                .orElseGet(() -> userService.createRouteUser(userEmail)));
        route.setName(routeDetailDto.getName());
        route.setRouteType(routeDetailDto.getRouteType());
        route.setTargetDistance(routeDetailDto.getTargetDistance());
        route.setDistanceMeters(routeDetailDto.getDistanceMeters());
        route.setElevationGain(routeDetailDto.getElevationGain());
        route.setElevationLoss(routeDetailDto.getElevationLoss());

        try {
            if (routeDetailDto.getStartPoint() != null) {
                Geometry startGeom = wktReader.read(routeDetailDto.getStartPoint());
                startGeom.setSRID(4326);
                route.setStartPoint((Point) startGeom);
            }

            if (routeDetailDto.getRouteGeom() != null) {
                Geometry routeGeom = wktReader.read(routeDetailDto.getRouteGeom());
                routeGeom.setSRID(4326);
                route.setRouteGeom((LineString) routeGeom);
            }
        } catch (org.locationtech.jts.io.ParseException e) {
            throw new RuntimeException("Błąd podczas parsowania geometrii WKT z frontendu: " + e.getMessage(), e);
        }

        route.setCreatedAt(routeDetailDto.getCreatedAt());
        return route;

    }



}