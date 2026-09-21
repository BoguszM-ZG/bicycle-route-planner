package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.AvoidanceZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AvoidanceZoneRepository extends JpaRepository<AvoidanceZone, Long> {
    Collection<AvoidanceZone> findByUserId(Long userId);
    Optional<AvoidanceZone> findByIdAndUserId(Long id, Long userId);
}