package com.bogusz.bicyclerouteplannerbackend.controller;


import com.bogusz.bicyclerouteplannerbackend.dto.auth.UserPrincipal;
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

@RestController
@RequestMapping("/api/avoidance-zones")
@RequiredArgsConstructor
public class AvoidanceZoneController {

    private final AvoidanceZoneService avoidanceZoneService;

    @GetMapping
    public ResponseEntity<List<AvoidanceZoneDto>> getAvoidanceZonesForUser(
            @AuthenticationPrincipal UserPrincipal principal){
        List<AvoidanceZoneDto> avoidanceZones = avoidanceZoneService.getAvoidanceZonesForUser(principal.id());
        return ResponseEntity.ok(avoidanceZones);
    }

    @PostMapping
    public ResponseEntity<AvoidanceZoneDto> createAvoidanceZone(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAvoidanceZoneRequestDto request){
        AvoidanceZoneDto savedAvoidanceZone = avoidanceZoneService.saveAvoidanceZoneForUser(request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAvoidanceZone);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvoidanceZone(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserPrincipal principal){
        avoidanceZoneService.deleteAvoidanceZone(id, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> updateAvoidanceZoneStatus(@PathVariable Long id,
           @Valid @RequestBody UpdateAvoidanceZoneStatusDto updateDto,
           @AuthenticationPrincipal UserPrincipal principal){
        avoidanceZoneService.toggleAvoidanceActiveStatus(id, principal.id(), updateDto.isActive());
        return ResponseEntity.noContent().build();
    }

}
