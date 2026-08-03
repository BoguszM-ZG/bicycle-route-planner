package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;


    public List<RouteDetailDto> getAllRoutes(){
        return routeRepository.findAll().stream()
                .map(this::toRouteDetailDto)
                .collect(Collectors.toList());
    }

    private RouteDetailDto toRouteDetailDto(Route route) {
        WKTWriter wtkWriter3D = new WKTWriter(3);

        return new RouteDetailDto(
                route.getId(),
                route.getUser().getEmail(),
                route.getName(),
                route.getRouteType(),
                route.getTargetDistance(),
                route.getDistanceMeters(),
                route.getElevationGain(),
                route.getElevationLoss(),
                route.getStartPoint() != null ? wtkWriter3D.write(route.getStartPoint()) : null,
                route.getRouteGeom() != null ? wtkWriter3D.write(route.getRouteGeom()) : null,
                route.getCreatedAt()
        );
    }
}
