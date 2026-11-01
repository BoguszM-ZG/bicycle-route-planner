package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.dto.route.ElevationPointDto;
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

    @Query(value = """
            SELECT ST_AsGeoJSON(r.*) FROM (
                SELECT name, distance_meters, elevation_gain, elevation_loss, route_geom
                FROM routes WHERE id = :id
            ) r""", nativeQuery = true)
    String findGeoJsonById(@Param("id") Long id);

    @Query(value = """
            WITH points AS (
                SELECT dp.path[1] AS idx, dp.geom
                FROM routes r, ST_DumpPoints(r.route_geom) dp
                WHERE r.id = :id
            ),
            steps AS (
                SELECT idx,
                       ST_Z(geom) AS elevation,
                       COALESCE(ST_Distance(CAST(geom AS geography),
                                            CAST(LAG(geom) OVER (ORDER BY idx) AS geography)), 0) AS step
                FROM points
            )
            SELECT SUM(step) OVER (ORDER BY idx) AS distance, elevation
            FROM steps
            ORDER BY idx""", nativeQuery = true)
    List<ElevationPointDto> findElevationProfileById(@Param("id") Long id);
}