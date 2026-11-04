package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.*;
import com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.exception.RouteNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.mapper.RouteMapper;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.AvoidanceZoneRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.FolderRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private static final int MAX_ELEVATION_PROFILE_POINTS = 250;

    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final GpxExportService gpxExportService;
    private final FolderRepository folderRepository;
    private final AvoidanceZoneRepository avoidanceZoneRepository;
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

    @Transactional(readOnly = true)
    public FileExportDto exportRouteToGpx(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        byte[] gpxData = gpxExportService.generateGpxFile(route);
        return new FileExportDto(gpxData, resolveFileName(route.getName(), "gpx"));
    }

    @Transactional(readOnly = true)
    public FileExportDto exportRouteToGeoJson(Long routeId, Long userId) {
        Route route = getValidRouteForUser(routeId, userId);
        String geoJson = routeRepository.findGeoJsonById(routeId);
        return new FileExportDto(geoJson.getBytes(StandardCharsets.UTF_8), resolveFileName(route.getName(), "geojson"));
    }

    @Transactional(readOnly = true)
    public List<ElevationPointDto> getElevationPointsForRoute(Long routeId, Long userId) {
        getValidRouteForUser(routeId, userId);
        List<ElevationPointDto> elevationProfile = routeRepository.findElevationProfileById(routeId);
        return elevationProfileDownsampler.downsample(elevationProfile, MAX_ELEVATION_PROFILE_POINTS);
    }

    @Transactional(readOnly = true)
    public List<Long> getIntersectingZoneIds(Long routeId, Long userId) {
        getValidRouteForUser(routeId, userId);
        return avoidanceZoneRepository.findIdsIntersectingRoute(routeId, userId);
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