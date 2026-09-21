package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteSummaryDto;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT new com.bogusz.bicyclerouteplannerbackend.dto.route.RouteSummaryDto(" +
            "r.id, r.name, r.distanceMeters, r.folder.id) " +
            "FROM Route r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<RouteSummaryDto> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT new com.bogusz.bicyclerouteplannerbackend.dto.route.RouteSummaryDto(" +
            "r.id, r.name, r.distanceMeters, r.folder.id) " +
            "FROM Route r WHERE r.user.id = :userId " +
            "AND r.folder.id = :folderId ORDER BY r.createdAt DESC")
    List<RouteSummaryDto> findAllByUserIdAndFolderIdOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                                         @Param("folderId") Long folderId);
}