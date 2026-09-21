package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.*;
import com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.exception.InvalidGeometryException;
import com.bogusz.bicyclerouteplannerbackend.exception.RouteNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.repository.FolderRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;



@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final GpxExportService gpxExportService;
    private final FolderRepository folderRepository;


    @Transactional(readOnly = true)
    public List<RouteSummaryDto> getRoutesForUser(Long userId, Long folderId) {
        if (folderId == null){
            return routeRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        } else {
            return routeRepository.findAllByUserIdAndFolderIdOrderByCreatedAtDesc(userId, folderId);
        }
    }

    @Transactional(readOnly = true)
    public RouteDetailDto getRouteDetail(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        return toRouteDetailDto(route);
    }

    @Transactional
    public RouteDetailDto saveRouteForUser(CreateRouteRequestDto createRouteRequestDto, Long userId) {
        Route route = createRouteRequestDtoToRoute(createRouteRequestDto, userId);
        route = routeRepository.save(route);
        return toRouteDetailDto(route);
    }

    @Transactional
    public void deleteRouteForUser(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        routeRepository.delete(route);
    }

    @Transactional(readOnly = true)
    public GpxExportDto exportRouteToGpx(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        byte[] gpxData = gpxExportService.generateGpxFile(route);
        return new GpxExportDto(gpxData, route.getName());
    }


    @Transactional(readOnly = true)
    public List<ElevationPointDto> getElevationPointsForRoute(Long routeId, Long userId){
        Route route = getValidRouteForUser(routeId, userId);
        Coordinate[] coordinates = route.getRouteGeom().getCoordinates();
        List<ElevationPointDto> elevationProfile = new ArrayList<>();
        double totalDistance = 0.0;

        for (int i = 0; i < coordinates.length; i++) {
            Coordinate current = coordinates[i];

            if (i > 0) {
                Coordinate previous = coordinates[i - 1];
                double distanceMeters = calculateHaversineDistance(
                        previous.getY(), previous.getX(),
                        current.getY(), current.getX()
                );
                totalDistance += distanceMeters;
            }
            if (!Double.isNaN(current.getZ())) {
                elevationProfile.add(new ElevationPointDto(totalDistance, current.getZ()));
            }
        }

        return elevationProfile;
    }

    @Transactional
    public void updateRouteFolder(Long routeId, UpdateRouteFolderDto request, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);

        if (request.folderId() == null) {
            route.setFolder(null);
        } else {
            route.setFolder(folderRepository.findByIdAndUserId(request.folderId(), userId).orElseThrow(
                    () -> new FolderNotFoundException("Folder not found or access denied")));
        }
    }

    @Transactional
    public void renameRoute(Long routeId, RenameRouteRequestDto request, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        route.setName(request.name());
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



    private Route getValidRouteForUser(Long routeId, Long userId){
        return routeRepository.findByIdAndUserId(routeId, userId)
                .orElseThrow(() -> new RouteNotFoundException("Route not found or access denied"));
    }


    private RouteDetailDto toRouteDetailDto(Route route) {
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


    private Route createRouteRequestDtoToRoute(CreateRouteRequestDto createRouteRequestDto, Long userId) {
        WKTReader wktReader = new WKTReader();
        Route route = new Route();

        route.setUser(userRepository.getReferenceById(userId));
        route.setName(createRouteRequestDto.name());
        route.setRouteType(createRouteRequestDto.routeType());
        route.setTargetDistance(createRouteRequestDto.targetDistance());
        route.setDistanceMeters(createRouteRequestDto.distanceMeters());
        route.setElevationGain(createRouteRequestDto.elevationGain());
        route.setElevationLoss(createRouteRequestDto.elevationLoss());

        try {
            if (createRouteRequestDto.startPoint() != null) {
                Geometry startGeom = wktReader.read(createRouteRequestDto.startPoint());
                startGeom.setSRID(4326);
                route.setStartPoint((Point) startGeom);
            }

            if (createRouteRequestDto.routeGeom() != null) {
                Geometry routeGeom = wktReader.read(createRouteRequestDto.routeGeom());
                routeGeom.setSRID(4326);
                route.setRouteGeom((LineString) routeGeom);
            }
        } catch (ParseException e) {
            throw new InvalidGeometryException("Error parsing WKT geometry: " + e.getMessage());
        }

        if (createRouteRequestDto.folderId() != null) {
            Folder folder = folderRepository.findByIdAndUserId(createRouteRequestDto.folderId(), userId)
                    .orElseThrow(() -> new FolderNotFoundException("Folder not found or access denied"));
            route.setFolder(folder);
        }


        return route;
    }


}