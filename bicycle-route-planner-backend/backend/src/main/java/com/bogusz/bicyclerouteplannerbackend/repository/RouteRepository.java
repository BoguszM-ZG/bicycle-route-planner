package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByIdAndUser_Email(Long id, String email);
    List<Route> findAllByUser_EmailOrderByCreatedAtDesc(String email);
    List<Route> findAllByUser_EmailAndFolder_IdOrderByCreatedAtDesc(String email, Long folderId);
}