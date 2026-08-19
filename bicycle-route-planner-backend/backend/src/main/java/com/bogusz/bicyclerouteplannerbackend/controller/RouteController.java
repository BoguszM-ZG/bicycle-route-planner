package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteDetailDto>> getAllRoutes(Authentication authentication) {
        List<RouteDetailDto> routes = routeService.getRoutesForUser(authentication.getName());
        return ResponseEntity.ok(routes);
    }

    @PostMapping
    public ResponseEntity<RouteDetailDto> saveRoute(@RequestBody RouteDetailDto routeDetailDto,
                                                    Authentication authentication) {
        RouteDetailDto savedRoute = routeService.saveRouteForUser(routeDetailDto, authentication.getName());
        return ResponseEntity.ok(savedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id, Authentication authentication){
        routeService.deleteRouteForUser(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
