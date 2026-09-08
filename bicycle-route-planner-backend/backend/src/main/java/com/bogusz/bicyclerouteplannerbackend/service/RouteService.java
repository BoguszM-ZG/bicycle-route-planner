package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.route.ElevationPointDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.GpxExportDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final GpxExportService gpxExportService;


    public List<RouteDetailDto> getRoutesForUser(String userEmail) {
        return routeRepository.findAllByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toRouteDetailDto)
                .toList();
    }

    public RouteDetailDto saveRouteForUser(RouteDetailDto routeDetailDto, String userEmail) {
        Route route = toRoute(routeDetailDto, userEmail);
        route = routeRepository.save(route);
        return toRouteDetailDto(route);
    }

    @Transactional
    public void deleteRouteForUser(Long routeId, String userEmail) {
        Route route = getValidRouteForUser(routeId, userEmail);
        routeRepository.delete(route);
    }

    public GpxExportDto exportRouteToGpx(Long routeId, String userEmail) {
        Route route = getValidRouteForUser(routeId, userEmail);
        byte[] gpxData = gpxExportService.generateGpxFile(route);
        return new GpxExportDto(gpxData, route.getName());
    }


    public List<ElevationPointDto> getElevationPointsForRoute(Long routeId, String userEmail){
        Route route = getValidRouteForUser(routeId, userEmail);
        Coordinate[] coordinates = route.getRouteGeom().getCoordinates();
        List<ElevationPointDto> elevationProfile = new ArrayList<>();
        double totalDistance = 0.0;

        for(int i = 0; i < coordinates.length; i++){
            Coordinate current = coordinates[i];

            if (Double.isNaN(current.getZ())){
                continue;
            }

            if(i > 0){
                Coordinate previous = coordinates[i - 1];
                double distanceMeters = calculateHaversineDistance(
                        previous.getY(), previous.getX(),
                        current.getY(), current.getX()
                );
                totalDistance += distanceMeters;
            }
            elevationProfile.add(new ElevationPointDto(totalDistance, current.getZ()));

        }

        return elevationProfile;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }


    private Route getValidRouteForUser(Long routeId, String userEmail){
        Route route = routeRepository.findRouteById(routeId);

        if (route == null) {
            throw new IllegalArgumentException("Route not found with id: " + routeId);
        }

        if (route.getUser() == null || !route.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Access denied, you are not the owner of this route");
        }
        return route;
    }


    private RouteDetailDto toRouteDetailDto(Route route) {
        WKTWriter wtkWriter3D = new WKTWriter(3);

        return RouteDetailDto.builder()
                .id(route.getId())
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
        route.setName(routeDetailDto.name());
        route.setRouteType(routeDetailDto.routeType());
        route.setTargetDistance(routeDetailDto.targetDistance());
        route.setDistanceMeters(routeDetailDto.distanceMeters());
        route.setElevationGain(routeDetailDto.elevationGain());
        route.setElevationLoss(routeDetailDto.elevationLoss());

        try {
            if (routeDetailDto.startPoint() != null) {
                Geometry startGeom = wktReader.read(routeDetailDto.startPoint());
                startGeom.setSRID(4326);
                route.setStartPoint((Point) startGeom);
            }

            if (routeDetailDto.routeGeom() != null) {
                Geometry routeGeom = wktReader.read(routeDetailDto.routeGeom());
                routeGeom.setSRID(4326);
                route.setRouteGeom((LineString) routeGeom);
            }
        } catch (org.locationtech.jts.io.ParseException e) {
            throw new RuntimeException("Error parsing WKT geometry: " + e.getMessage(), e);
        }

        route.setCreatedAt(Optional.ofNullable(routeDetailDto.createdAt()).orElseGet(Instant::now));
        return route;
    }


}