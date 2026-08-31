package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.AvoidanceZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface AvoidanceZoneRepository extends JpaRepository<AvoidanceZone, Long> {
    Collection<AvoidanceZone> findByUserEmail(String userEmail);

    AvoidanceZone findAvoidanceZoneById(Long id);
}