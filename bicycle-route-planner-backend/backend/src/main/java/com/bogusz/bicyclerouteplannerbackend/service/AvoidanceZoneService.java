package com.bogusz.bicyclerouteplannerbackend.service;


import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.CreateAvoidanceZoneRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.AvoidanceZoneNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.exception.InvalidGeometryException;
import com.bogusz.bicyclerouteplannerbackend.model.AvoidanceZone;
import com.bogusz.bicyclerouteplannerbackend.repository.AvoidanceZoneRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public List<AvoidanceZoneDto> getAvoidanceZonesForUser(Long userId){
        return avoidanceZoneRepository.findByUserId(userId).stream()
                .map(this::toAvoidanceZoneDto)
                .toList();
    }

    @Transactional
    public AvoidanceZoneDto saveAvoidanceZoneForUser(CreateAvoidanceZoneRequestDto request, Long userId){
        AvoidanceZone avoidanceZone = createAvoidanceZoneRequestDtoToAvoidanceZone(request, userId);
        avoidanceZone = avoidanceZoneRepository.save(avoidanceZone);
        return toAvoidanceZoneDto(avoidanceZone);
    }

    @Transactional
    public void deleteAvoidanceZone(Long avoidanceZoneId, Long userId){
        AvoidanceZone avoidanceZone = getValidAvoidanceZoneForUser(avoidanceZoneId, userId);
        avoidanceZoneRepository.delete(avoidanceZone);
    }

    @Transactional
    public void toggleAvoidanceActiveStatus(Long avoidanceZoneId, Long userId, boolean isActive){
        AvoidanceZone avoidanceZone = getValidAvoidanceZoneForUser(avoidanceZoneId, userId);
        avoidanceZone.setIsActive(isActive);
    }

    private AvoidanceZone getValidAvoidanceZoneForUser(Long avoidanceZoneId, Long userId) {
        return avoidanceZoneRepository.findByIdAndUserId(avoidanceZoneId, userId)
                .orElseThrow(() -> new AvoidanceZoneNotFoundException("Avoidance Zone not found or access denied"));
    }


    private AvoidanceZone createAvoidanceZoneRequestDtoToAvoidanceZone(
            CreateAvoidanceZoneRequestDto request, Long userId){
        WKTReader wktReader = new WKTReader();
        AvoidanceZone avoidanceZone = new AvoidanceZone();

        avoidanceZone.setUser(userRepository.getReferenceById(userId));
        avoidanceZone.setName(request.name());
        try {
            if (request.zoneGeom() != null) {
                Geometry geom = wktReader.read(request.zoneGeom());
                geom.setSRID(4326);
                avoidanceZone.setZoneGeom((Polygon) geom);
            }
        } catch (ParseException e) {
            throw new InvalidGeometryException("Error parsing WKT geometry " + e.getMessage());
        }

        avoidanceZone.setIsActive(true);
        return avoidanceZone;
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

}
