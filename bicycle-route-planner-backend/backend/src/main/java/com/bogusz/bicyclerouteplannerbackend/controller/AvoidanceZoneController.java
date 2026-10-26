package com.bogusz.bicyclerouteplannerbackend.controller;


import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.CreateAvoidanceZoneRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.UpdateAvoidanceZoneStatusDto;
import com.bogusz.bicyclerouteplannerbackend.service.AvoidanceZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/avoidance-zones")
@RequiredArgsConstructor
@Tag(name = "Avoidance Zones", description = "Management of map areas to avoid during routing")
public class AvoidanceZoneController {

    private final AvoidanceZoneService avoidanceZoneService;

    @GetMapping
    @Operation(summary = "Get avoidance zones", description = "Retrieves all avoidance zones for the authenticated user.")
    public ResponseEntity<List<AvoidanceZoneDto>> getAvoidanceZonesForUser(
            @AuthenticationPrincipal UserPrincipal principal){
        List<AvoidanceZoneDto> avoidanceZones = avoidanceZoneService.getAvoidanceZonesForUser(principal.id());
        return ResponseEntity.ok(avoidanceZones);
    }

    @PostMapping
    @Operation(summary = "Create avoidance zone", description = "Saves a new avoidance area geometry.")
    public ResponseEntity<AvoidanceZoneDto> createAvoidanceZone(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAvoidanceZoneRequestDto request){
        AvoidanceZoneDto savedAvoidanceZone = avoidanceZoneService.saveAvoidanceZoneForUser(request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAvoidanceZone);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete avoidance zone", description = "Removes a specific avoidance zone.")
    public ResponseEntity<Void> deleteAvoidanceZone(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserPrincipal principal){
        avoidanceZoneService.deleteAvoidanceZone(id, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Toggle zone status", description = "Enables or disables an avoidance zone without deleting it.")
    public ResponseEntity<Void> updateAvoidanceZoneStatus(@PathVariable Long id,
           @Valid @RequestBody UpdateAvoidanceZoneStatusDto updateDto,
           @AuthenticationPrincipal UserPrincipal principal){
        avoidanceZoneService.toggleAvoidanceActiveStatus(id, principal.id(), updateDto.isActive());
        return ResponseEntity.noContent().build();
    }

}
