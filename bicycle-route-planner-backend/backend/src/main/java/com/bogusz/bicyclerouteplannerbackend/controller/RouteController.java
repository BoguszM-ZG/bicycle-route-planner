package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.*;
import com.bogusz.bicyclerouteplannerbackend.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteSummaryDto>> getAllRoutes(
            @RequestParam(value = "folderId", required = false) Long folderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RouteSummaryDto> routes = routeService.getRoutesForUser(principal.id(), folderId);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteDetailDto> getRouteDetail(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserPrincipal principal){
        RouteDetailDto routeDetail = routeService.getRouteDetail(id, principal.id());
        return ResponseEntity.ok(routeDetail);
    }

    @PostMapping
    public ResponseEntity<RouteDetailDto> saveRoute( @Valid @RequestBody CreateRouteRequestDto createRouteRequestDto,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        RouteDetailDto savedRoute = routeService.saveRouteForUser(createRouteRequestDto, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal){
        routeService.deleteRouteForUser(id, principal.id());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export/gpx")
    public ResponseEntity<byte[]> exportRouteToGpx(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserPrincipal principal){

        GpxExportDto exportData = routeService.exportRouteToGpx(id, principal.id());

        String safeFileName = exportData.routeName().replaceAll("[^a-zA-Z0-9\\-ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]", "_") + ".gpx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", safeFileName);
        return ResponseEntity.ok()
                .headers(headers)
                .body(exportData.gpxData());
    }

    @GetMapping("/{id}/elevation")
    public ResponseEntity<List<ElevationPointDto>> getElevationPoints(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserPrincipal principal){
        List<ElevationPointDto> elevationPoints = routeService.getElevationPointsForRoute(id, principal.id());
        return ResponseEntity.ok(elevationPoints);
    }

    @PatchMapping("/{id}/folder")
    public ResponseEntity<Void> updateRouteFolder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateRouteFolderDto request
    ) {
        routeService.updateRouteFolder(id, request, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<Void> renameRoute(
            @PathVariable Long id,
            @Valid @RequestBody RenameRouteRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        routeService.renameRoute(id, request, principal.id());
        return ResponseEntity.noContent().build();
    }
}
