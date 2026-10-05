package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
@Tag(name = "Geocoding", description = "Forward geocoding and location search proxy")
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping(value = "/search", produces = "application/json")
    @Operation(summary = "Search location", description = "Proxies requests to an external geocoding service.")
    public ResponseEntity<String> search(
            @RequestParam String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        String response = geocodingService.searchLocation(q, lat, lon);
        return ResponseEntity.ok(response);
    }
}
