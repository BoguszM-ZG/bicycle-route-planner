package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.service.ExternalRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
@Tag(name = "External Routing", description = "Proxy for external routing engine (e.g. OpenRouteService)")
public class RoutingController {

    private final ExternalRoutingService externalRoutingService;

    @PostMapping(value = "/directions", produces = "application/json")
    @Operation(summary = "Get directions", description = "Calculates route directions using external routing provider.")
    public ResponseEntity<String> getDirections(@RequestBody OrsRouteRequestDto request) {
        String response = externalRoutingService.getDirections(request);
        return ResponseEntity.ok(response);
    }
}