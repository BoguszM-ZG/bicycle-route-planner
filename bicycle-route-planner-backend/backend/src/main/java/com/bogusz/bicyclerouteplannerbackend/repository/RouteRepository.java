package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Route findRouteById(Long id);
    List<Route> findAllByUser_EmailAndFolderIsNullOrderByCreatedAtDesc(String email);
    List<Route> findAllByUser_EmailAndFolder_IdOrderByCreatedAtDesc(String email, Long folderId);
}