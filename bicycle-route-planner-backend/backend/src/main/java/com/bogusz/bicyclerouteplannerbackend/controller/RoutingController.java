package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.service.ExternalRoutingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
public class RoutingController {

    private final ExternalRoutingService externalRoutingService;

    @PostMapping("/directions")
    public ResponseEntity<JsonNode> getDirections(@RequestBody OrsRouteRequestDto request) {
        JsonNode response = externalRoutingService.getDirections(request);
        return ResponseEntity.ok(response);
    }
}