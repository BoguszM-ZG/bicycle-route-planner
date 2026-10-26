package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.geocoding.GeocodingSearchRequestDto;
import com.bogusz.bicyclerouteplannerbackend.service.GeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
@Tag(name = "Geocoding", description = "Forward geocoding and location search proxy")
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping(value = "/search", produces = "application/json")
    @Operation(summary = "Search location", description = "Proxies requests to an external geocoding service.")
    public ResponseEntity<String> search(@Valid @ParameterObject GeocodingSearchRequestDto request) {
        String response = geocodingService.searchLocation(request);
        return ResponseEntity.ok(response);
    }
}
