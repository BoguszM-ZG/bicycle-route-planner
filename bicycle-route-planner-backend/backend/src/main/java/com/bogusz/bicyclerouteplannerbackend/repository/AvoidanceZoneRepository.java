package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.AvoidanceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface AvoidanceZoneRepository extends JpaRepository<AvoidanceZone, Long> {
    Collection<AvoidanceZone> findByUserId(Long userId);
    Optional<AvoidanceZone> findByIdAndUserId(Long id, Long userId);

    @Query(value = """
            SELECT GREATEST(ST_Distance(sw, se), ST_Distance(sw, nw))
            FROM (SELECT CAST(ST_SetSRID(ST_MakePoint(ST_XMin(g), ST_YMin(g)), 4326) AS geography) AS sw,
                         CAST(ST_SetSRID(ST_MakePoint(ST_XMax(g), ST_YMin(g)), 4326) AS geography) AS se,
                         CAST(ST_SetSRID(ST_MakePoint(ST_XMin(g), ST_YMax(g)), 4326) AS geography) AS nw
                  FROM (SELECT ST_GeomFromText(:wkt, 4326) AS g) geom) corners""", nativeQuery = true)
    double calculateMaxExtentMeters(@Param("wkt") String wkt);

    @Query(value = "SELECT ST_Area(CAST(ST_GeomFromText(:wkt, 4326) AS geography))", nativeQuery = true)
    double calculateAreaSquareMeters(@Param("wkt") String wkt);

    @Query(value = """
            SELECT COALESCE(SUM(ST_Area(CAST(zone_geom AS geography))), 0)
            FROM avoidance_zones
            WHERE user_id = :userId AND (is_active OR id = :includedZoneId)""", nativeQuery = true)
    double sumActiveAreaSquareMeters(@Param("userId") Long userId, @Param("includedZoneId") Long includedZoneId);
}