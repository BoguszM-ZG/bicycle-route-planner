package com.bogusz.bicyclerouteplannerbackend.service;


import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.CreateAvoidanceZoneRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.AvoidanceZoneLimitExceededException;
import com.bogusz.bicyclerouteplannerbackend.exception.AvoidanceZoneNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.mapper.AvoidanceZoneMapper;
import com.bogusz.bicyclerouteplannerbackend.model.AvoidanceZone;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.AvoidanceZoneRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvoidanceZoneService {
    private static final double MAX_ZONE_EXTENT_METERS = 20_000;
    private static final double MAX_ACTIVE_ZONES_AREA_SQUARE_METERS = 200_000_000;

    private final UserRepository userRepository;
    private final AvoidanceZoneRepository avoidanceZoneRepository;
    private final AvoidanceZoneMapper avoidanceZoneMapper;

    @Transactional(readOnly = true)
    public List<AvoidanceZoneDto> getAvoidanceZonesForUser(Long userId){
        return avoidanceZoneRepository.findByUserId(userId).stream()
                .map(avoidanceZoneMapper::toDto)
                .toList();
    }

    @Transactional
    public AvoidanceZoneDto saveAvoidanceZoneForUser(CreateAvoidanceZoneRequestDto request, Long userId){
        String zoneGeom = request.zoneGeom();
        if (avoidanceZoneRepository.calculateMaxExtentMeters(zoneGeom) > MAX_ZONE_EXTENT_METERS) {
            throw new AvoidanceZoneLimitExceededException("Avoidance zone width and height cannot exceed 20 km");
        }
        double totalActiveArea = avoidanceZoneRepository.sumActiveAreaSquareMeters(userId, null)
                + avoidanceZoneRepository.calculateAreaSquareMeters(zoneGeom);
        if (totalActiveArea > MAX_ACTIVE_ZONES_AREA_SQUARE_METERS) {
            throw new AvoidanceZoneLimitExceededException("Total area of active avoidance zones cannot exceed 200 km²");
        }
        User user = userRepository.getReferenceById(userId);
        AvoidanceZone avoidanceZone = avoidanceZoneMapper.toEntity(request, user);
        avoidanceZone = avoidanceZoneRepository.save(avoidanceZone);
        log.info("Saved new avoidance zone with ID: {} for user ID: {}", avoidanceZone.getId(), userId);
        return avoidanceZoneMapper.toDto(avoidanceZone);
    }

    @Transactional
    public void deleteAvoidanceZone(Long avoidanceZoneId, Long userId){
        AvoidanceZone avoidanceZone = getValidAvoidanceZoneForUser(avoidanceZoneId, userId);
        avoidanceZoneRepository.delete(avoidanceZone);
        log.info("Deleted avoidance zone with ID: {} for user ID: {}", avoidanceZoneId, userId);
    }

    @Transactional
    public void toggleAvoidanceActiveStatus(Long avoidanceZoneId, Long userId, boolean isActive){
        AvoidanceZone avoidanceZone = getValidAvoidanceZoneForUser(avoidanceZoneId, userId);
        boolean isBeingActivated = isActive && !Boolean.TRUE.equals(avoidanceZone.getIsActive());
        if (isBeingActivated && avoidanceZoneRepository.sumActiveAreaSquareMeters(userId, avoidanceZoneId)
                > MAX_ACTIVE_ZONES_AREA_SQUARE_METERS) {
            throw new AvoidanceZoneLimitExceededException("Total area of active avoidance zones cannot exceed 200 km²");
        }
        avoidanceZone.setIsActive(isActive);
    }

    private AvoidanceZone getValidAvoidanceZoneForUser(Long avoidanceZoneId, Long userId) {
        return avoidanceZoneRepository.findByIdAndUserId(avoidanceZoneId, userId)
                .orElseThrow(() -> new AvoidanceZoneNotFoundException("Avoidance Zone not found or access denied"));
    }

}
