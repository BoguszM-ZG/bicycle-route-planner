package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
}