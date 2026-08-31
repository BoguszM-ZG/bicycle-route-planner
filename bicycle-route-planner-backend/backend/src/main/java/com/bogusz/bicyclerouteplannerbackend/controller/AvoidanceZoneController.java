package com.bogusz.bicyclerouteplannerbackend.controller;


import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.service.AvoidanceZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping({"/api/avoidance-zones"})
@RequiredArgsConstructor
public class AvoidanceZoneController {

    private final AvoidanceZoneService avoidanceZoneService;

    @GetMapping
    public ResponseEntity<List<AvoidanceZoneDto>> getAvoidanceZonesForUser(Authentication authentication){
        List<AvoidanceZoneDto> avoidanceZones = avoidanceZoneService.getAvoidanceZonesForUser(authentication.getName());
        return ResponseEntity.ok(avoidanceZones);
    }

    @PostMapping
    public ResponseEntity<AvoidanceZoneDto> createAvoidanceZone(@RequestBody AvoidanceZoneDto avoidanceZoneDto, Authentication authentication){
        AvoidanceZoneDto savedAvoidanceZone = avoidanceZoneService.saveAvoidanceZoneForUser(avoidanceZoneDto, authentication.getName());
        return ResponseEntity.ok(savedAvoidanceZone);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvoidanceZone(@PathVariable Long id, Authentication authentication){
        avoidanceZoneService.deleteAvoidanceZone(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> toggleAvoidanceActiveStatus(@PathVariable Long id, @RequestParam boolean isActive, Authentication authentication){
        avoidanceZoneService.toggleAvoidanceActiveStatus(id, authentication.getName(), isActive);
        return ResponseEntity.noContent().build();
    }

}
