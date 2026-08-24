package com.bogusz.bicyclerouteplannerbackend.service;


import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.model.AvoidanceZone;
import com.bogusz.bicyclerouteplannerbackend.repository.AvoidanceZoneRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AvoidanceZoneService {
    private final UserRepository userRepository;
    private final AvoidanceZoneRepository avoidanceZoneRepository;

    public List<AvoidanceZoneDto> getAvoidanceZonesForUser(String userEmail){
        return avoidanceZoneRepository.findByUserEmail(userEmail).stream()
                .map(this::toAvoidanceZoneDto)
                .toList();
    }

    public AvoidanceZoneDto saveAvoidanceZoneForUser(AvoidanceZoneDto avoidanceZoneDto, String userEmail){
        AvoidanceZone avoidanceZone = toAvoidanceZone(avoidanceZoneDto, userEmail);
        avoidanceZone = avoidanceZoneRepository.save(avoidanceZone);
        return toAvoidanceZoneDto(avoidanceZone);
    }

    @Transactional
    public void deleteAvoidanceZone(Long avoidanceZoneId, String userEmail){
        AvoidanceZone avoidanceZone = avoidanceZoneRepository.findAvoidanceZoneById(avoidanceZoneId);

        if (avoidanceZone == null){
            throw new IllegalArgumentException("Avoidance Zone not found with id: " + avoidanceZoneId);
        }

        if (!avoidanceZone.getUser().getEmail().equals(userEmail)){
            throw new IllegalArgumentException("Access denied , you are not the owner of this avoidance zone");
        }

        avoidanceZoneRepository.delete(avoidanceZone);
    }

    @Transactional
    public void toggleAvoidanceActiveStatus(Long avoidanceZoneId, String userEmail, boolean isActive){
        AvoidanceZone avoidanceZone = avoidanceZoneRepository.findAvoidanceZoneById(avoidanceZoneId);

        if (avoidanceZone == null){
            throw new IllegalArgumentException("Avoidance Zone not found with id: " + avoidanceZoneId);
        }

        if (!avoidanceZone.getUser().getEmail().equals(userEmail)){
            throw new IllegalArgumentException("Access denied , you are not the owner of this avoidance zone");
        }

        avoidanceZone.setIsActive(isActive);
        avoidanceZoneRepository.save(avoidanceZone);
    }




    private AvoidanceZoneDto toAvoidanceZoneDto(AvoidanceZone avoidanceZone){
        WKTWriter wktWriter = new WKTWriter();
        return AvoidanceZoneDto.builder()
                .id(avoidanceZone.getId())
                .name(avoidanceZone.getName())
                .zoneGeom(avoidanceZone.getZoneGeom() != null ? wktWriter.write(avoidanceZone.getZoneGeom()) : null)
                .isActive(avoidanceZone.getIsActive())
                .build();
    }

    private AvoidanceZone toAvoidanceZone(AvoidanceZoneDto avoidanceZoneDto, String userEmail){
        WKTReader wktReader = new WKTReader();
        AvoidanceZone avoidanceZone = new AvoidanceZone();

        avoidanceZone.setId(avoidanceZoneDto.getId());
        avoidanceZone.setUser(userRepository.findByEmail(userEmail).orElseThrow(() -> new RuntimeException("User not found " + userEmail)));
        avoidanceZone.setName(avoidanceZoneDto.getName());

        try {
            if (avoidanceZoneDto.getZoneGeom() != null) {
                Geometry geom = wktReader.read(avoidanceZoneDto.getZoneGeom());
                geom.setSRID(4326);
                avoidanceZone.setZoneGeom((Polygon) geom);
            }
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing WKT geometry " + e.getMessage(), e);
        }
        avoidanceZone.setIsActive(avoidanceZoneDto.getIsActive() != Boolean.FALSE);

        return avoidanceZone;
    }
}
