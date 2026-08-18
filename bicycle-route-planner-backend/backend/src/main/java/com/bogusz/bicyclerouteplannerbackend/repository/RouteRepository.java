package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findAllByUserEmailOrderByCreatedAtDesc(String email);

    Route findRouteById(Long id);
}