package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;


    public List<RouteDetailDto> getRoutesForUser(String userEmail){
        return routeRepository.findAllByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toRouteDetailDto)
                .collect(Collectors.toList());
    }

    public RouteDetailDto saveRouteForUser(RouteDetailDto routeDetailDto, String userEmail) {
        Route route = toRoute(routeDetailDto, userEmail);
        route = routeRepository.save(route);
        return toRouteDetailDto(route);
    }

    @Transactional
    public void deleteRouteForUser(Long routeId, String userEmail) {
        Route route = routeRepository.findRouteById(routeId);

        if (route == null){
            throw new IllegalArgumentException("Route not found with id: " + routeId);
        }

        if (route.getUser() == null || !route.getUser().getEmail().equals(userEmail)){
            throw new IllegalArgumentException("Access denied, you are not the owner of this route");
        }
        routeRepository.delete(route);


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

    private Route toRoute(RouteDetailDto routeDetailDto, String userEmail) {
        WKTReader wktReader = new WKTReader();
        Route route = new Route();

        route.setUser(userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found: " + userEmail)));
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

        route.setCreatedAt(Optional.ofNullable(routeDetailDto.getCreatedAt()).orElseGet(Instant::now));
        return route;

    }



}