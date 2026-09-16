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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AvoidanceZoneService {
    private final UserRepository userRepository;
    private final AvoidanceZoneRepository avoidanceZoneRepository;

    @Transactional(readOnly = true)
    public List<AvoidanceZoneDto> getAvoidanceZonesForUser(String userEmail){
        return avoidanceZoneRepository.findByUserEmail(userEmail).stream()
                .map(this::toAvoidanceZoneDto)
                .toList();
    }

    @Transactional
    public AvoidanceZoneDto saveAvoidanceZoneForUser(CreateAvoidanceZoneRequestDto request, String userEmail){
        AvoidanceZone avoidanceZone = createAvoidanceZoneRequestDtoToAvoidanceZone(request, userEmail);
        avoidanceZone = avoidanceZoneRepository.save(avoidanceZone);
        return toAvoidanceZoneDto(avoidanceZone);
    }

    @Transactional
    public void deleteAvoidanceZone(Long avoidanceZoneId, String userEmail){
        AvoidanceZone avoidanceZone = getValidAvoidanceZoneForUser(avoidanceZoneId, userEmail);
        avoidanceZoneRepository.delete(avoidanceZone);
    }

    @Transactional
    public void toggleAvoidanceActiveStatus(Long avoidanceZoneId, String userEmail, boolean isActive){
        AvoidanceZone avoidanceZone = getValidAvoidanceZoneForUser(avoidanceZoneId, userEmail);
        avoidanceZone.setIsActive(isActive);
    }

    private AvoidanceZone getValidAvoidanceZoneForUser(Long avoidanceZoneId, String userEmail) {
        return avoidanceZoneRepository.findByIdAndUser_Email(avoidanceZoneId, userEmail)
                .orElseThrow(() -> new AvoidanceZoneNotFoundException("Avoidance Zone not found or access denied"));
    }


    private AvoidanceZone createAvoidanceZoneRequestDtoToAvoidanceZone(
            CreateAvoidanceZoneRequestDto request, String userEmail){
        WKTReader wktReader = new WKTReader();
        AvoidanceZone avoidanceZone = new AvoidanceZone();

        avoidanceZone.setUser(userRepository.findByEmail(userEmail).orElseThrow(
                () -> new UsernameNotFoundException("User not found " + userEmail)));
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
