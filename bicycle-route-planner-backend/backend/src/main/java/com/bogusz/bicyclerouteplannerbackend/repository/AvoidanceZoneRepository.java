package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.AvoidanceZone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvoidanceZoneRepository extends JpaRepository<AvoidanceZone, Long> {
}