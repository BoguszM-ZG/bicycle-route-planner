package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.service.ExternalRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
public class RoutingController {

    private final ExternalRoutingService externalRoutingService;

    @PostMapping(value = "/directions", produces = "application/json")
    public ResponseEntity<String> getDirections(@RequestBody OrsRouteRequestDto request) {
        String response = externalRoutingService.getDirections(request);
        return ResponseEntity.ok(response);
    }
}