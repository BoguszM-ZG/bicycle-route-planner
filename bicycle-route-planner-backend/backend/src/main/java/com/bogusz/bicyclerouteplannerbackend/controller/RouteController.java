package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteDetailDto>> getAllRoutes() {
        List<RouteDetailDto> routes = routeService.getAllRoutes();
        return ResponseEntity.ok(routes);
    }

    @PostMapping
    public ResponseEntity<RouteDetailDto> saveRoute(@RequestBody RouteDetailDto routeDetailDto) {
        RouteDetailDto savedRoute = routeService.saveRoute(routeDetailDto);
        return ResponseEntity.ok(savedRoute);
    }
}
