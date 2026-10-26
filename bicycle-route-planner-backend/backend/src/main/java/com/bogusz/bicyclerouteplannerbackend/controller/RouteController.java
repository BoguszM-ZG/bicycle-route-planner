package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.*;
import com.bogusz.bicyclerouteplannerbackend.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Tag(name = "Routes", description = "Management of bicycle routes, elevation data, and GPX exports")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    @Operation(summary = "Get user routes", description = "Retrieves all routes for the authenticated user.")
    public ResponseEntity<List<RouteSummaryDto>> getAllRoutes(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RouteSummaryDto> routes = routeService.getRoutesForUser(principal.id());
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get route details", description = "Retrieves full details and geometry of a specific route.")
    public ResponseEntity<RouteDetailDto> getRouteDetail(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserPrincipal principal){
        RouteDetailDto routeDetail = routeService.getRouteDetail(id, principal.id());
        return ResponseEntity.ok(routeDetail);
    }

    @PostMapping
    @Operation(summary = "Save route", description = "Saves a new bicycle route for the authenticated user.")
    public ResponseEntity<RouteDetailDto> saveRoute( @Valid @RequestBody CreateRouteRequestDto createRouteRequestDto,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        RouteDetailDto savedRoute = routeService.saveRouteForUser(createRouteRequestDto, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRoute);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete route", description = "Deletes a specific route.")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal){
        routeService.deleteRouteForUser(id, principal.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export/gpx")
    @Operation(summary = "Export route to GPX", description = "Generates and returns a GPX file for the specified route.")
    public ResponseEntity<byte[]> exportRouteToGpx(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserPrincipal principal){

        GpxExportDto exportData = routeService.exportRouteToGpx(id, principal.id());

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(exportData.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/gpx+xml"))
                .headers(headers -> headers.setContentDisposition(contentDisposition))
                .body(exportData.gpxData());
    }

    @GetMapping("/{id}/elevation")
    @Operation(summary = "Get elevation data", description = "Retrieves elevation profile points for a specific route.")
    public ResponseEntity<List<ElevationPointDto>> getElevationPoints(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        List<ElevationPointDto> elevationPoints = routeService.getElevationPointsForRoute(id, principal.id());
        return ResponseEntity.ok(elevationPoints);
    }

    @PatchMapping("/{id}/folder")
    @Operation(summary = "Move route to folder", description = "Moves a route to a different folder.")
    public ResponseEntity<Void> updateRouteFolder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateRouteFolderDto request
    ) {
        routeService.updateRouteFolder(id, request, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/name")
    @Operation(summary = "Rename route", description = "Updates the name of a specific route.")
    public ResponseEntity<Void> renameRoute(
            @PathVariable Long id,
            @Valid @RequestBody RenameRouteRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        routeService.renameRoute(id, request, principal.id());
        return ResponseEntity.noContent().build();
    }
}
