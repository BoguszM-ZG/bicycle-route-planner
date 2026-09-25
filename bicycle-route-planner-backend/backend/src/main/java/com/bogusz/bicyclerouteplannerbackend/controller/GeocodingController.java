package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping(value = "/search", produces = "application/json")
    public ResponseEntity<String> search(
            @RequestParam String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        String response = geocodingService.searchLocation(q, lat, lon);
        return ResponseEntity.ok(response);
    }
}
