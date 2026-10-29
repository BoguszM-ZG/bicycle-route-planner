package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.*;
import com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.exception.RouteNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.mapper.RouteMapper;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.FolderRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final GpxExportService gpxExportService;
    private final FolderRepository folderRepository;
    private final RouteMapper routeMapper;
    private final ElevationProfileDownsampler elevationProfileDownsampler;


    @Transactional(readOnly = true)
    public List<RouteSummaryDto> getRoutesForUser(Long userId) {
        return routeRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public RouteDetailDto getRouteDetail(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        return routeMapper.toRouteDetailDto(route);
    }

    @Transactional
    public RouteDetailDto saveRouteForUser(CreateRouteRequestDto request, Long userId) {
        Folder folder = null;
        if (request.folderId() != null) {
            folder = folderRepository.findByIdAndUserId(request.folderId(), userId)
                    .orElseThrow(() -> new FolderNotFoundException("Folder not found or access denied"));
        }
        User user = userRepository.getReferenceById(userId);
        
        Route route = routeMapper.toEntity(request, user, folder);
        route = routeRepository.save(route);
        log.info("Saved new route with ID: {} for user ID: {}", route.getId(), userId);
        return routeMapper.toRouteDetailDto(route);
    }

    @Transactional
    public void deleteRouteForUser(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        routeRepository.delete(route);
        log.info("Deleted route with ID: {} for user ID: {}", routeId, userId);
    }

    public FileExportDto exportRouteToGpx(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        byte[] gpxData = gpxExportService.generateGpxFile(route);
        return new FileExportDto(gpxData, resolveFileName(route.getName(), "gpx"));
    }

    public FileExportDto exportRouteToGeoJson(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        String geoJson = routeRepository.findGeoJsonById(routeId);
        return new FileExportDto(geoJson.getBytes(StandardCharsets.UTF_8), resolveFileName(route.getName(), "geojson"));
    }

    @Transactional(readOnly = true)
    public List<ElevationPointDto> getElevationPointsForRoute(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        if (route.getRouteGeom() == null) {
            return List.of();
        }
        Coordinate[] coordinates = route.getRouteGeom().getCoordinates();
        List<ElevationPointDto> elevationProfile = new ArrayList<>(coordinates.length);
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

        return elevationProfileDownsampler.downsample(elevationProfile, 250);
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
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double clampedA = Math.clamp(a, 0.0, 1.0);
        double c = 2 * Math.atan2(Math.sqrt(clampedA), Math.sqrt(1 - clampedA));
        return 6371000.0 * c;
    }

    private Route getValidRouteForUser(Long routeId, Long userId){
        return routeRepository.findByIdAndUserId(routeId, userId)
                .orElseThrow(() -> new RouteNotFoundException("Route not found or access denied"));
    }

    private String resolveFileName(String routeName, String extension) {
        String defaultFileName = "route." + extension;
        if (routeName == null || routeName.isBlank()) {
            return defaultFileName;
        }
        String sanitized = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]").matcher(routeName).replaceAll("_")
                .replace("..", "_")
                .trim();
        if (sanitized.isEmpty() || sanitized.replace("_", "").isBlank()) {
            return defaultFileName;
        }
        return sanitized + "." + extension;
    }

}