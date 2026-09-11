package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.*;
import com.bogusz.bicyclerouteplannerbackend.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteSummaryDto>> getAllRoutes(
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) {
        List<RouteSummaryDto> routes = routeService.getRoutesForUser(authentication.getName(), folderId);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteDetailDto> getRouteDetail(@PathVariable Long id, Authentication authentication){
        RouteDetailDto routeDetail = routeService.getRouteDetail(id, authentication.getName());
        return ResponseEntity.ok(routeDetail);
    }

    @PostMapping
    public ResponseEntity<RouteDetailDto> saveRoute( @Valid @RequestBody CreateRouteRequestDto createRouteRequestDto,
                                                    Authentication authentication) {
        RouteDetailDto savedRoute = routeService.saveRouteForUser(createRouteRequestDto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id, Authentication authentication){
        routeService.deleteRouteForUser(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export/gpx")
    public ResponseEntity<byte[]> exportRouteToGpx(@PathVariable Long id, Authentication authentication){

        GpxExportDto exportData = routeService.exportRouteToGpx(id, authentication.getName());

        String safeFileName = exportData.routeName().replaceAll("[^a-zA-Z0-9\\-ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]", "_") + ".gpx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", safeFileName);
        return ResponseEntity.ok()
                .headers(headers)
                .body(exportData.gpxData());
    }

    @GetMapping("/{id}/elevation")
    public ResponseEntity<List<ElevationPointDto>> getElevationPoints(@PathVariable Long id, Authentication authentication){
        List<ElevationPointDto> elevationPoints = routeService.getElevationPointsForRoute(id, authentication.getName());
        return ResponseEntity.ok(elevationPoints);
    }

    @PatchMapping("/{id}/folder")
    public ResponseEntity<Void> updateRouteFolder(
            @PathVariable Long id,
            @RequestBody UpdateRouteFolderDto request,
            Authentication authentication
    ) {
        routeService.updateRouteFolder(id, request, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
