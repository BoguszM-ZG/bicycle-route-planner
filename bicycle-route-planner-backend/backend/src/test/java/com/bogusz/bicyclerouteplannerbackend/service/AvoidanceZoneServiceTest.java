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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AvoidanceZoneServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ZONE_ID = 100L;
    private static final String ZONE_NAME = "Construction Site";
    private static final String ZONE_GEOM = "POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))";
    private static final double SQUARE_KM = 1_000_000;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AvoidanceZoneRepository avoidanceZoneRepository;

    @Mock
    private AvoidanceZoneMapper avoidanceZoneMapper;

    @InjectMocks
    private AvoidanceZoneService avoidanceZoneService;

    @Test
    void shouldGetAvoidanceZonesForUserSuccessfully() {
        // Given
        AvoidanceZone mockZone = new AvoidanceZone();
        AvoidanceZoneDto mockDto = mock(AvoidanceZoneDto.class);

        given(avoidanceZoneRepository.findByUserId(USER_ID)).willReturn(List.of(mockZone));
        given(avoidanceZoneMapper.toDto(mockZone)).willReturn(mockDto);

        // When
        List<AvoidanceZoneDto> result = avoidanceZoneService.getAvoidanceZonesForUser(USER_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(mockDto);
    }

    @Test
    void shouldSaveAvoidanceZoneForUserSuccessfully() {
        // Given
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(ZONE_NAME, ZONE_GEOM);
        User mockUser = new User();
        AvoidanceZone mockZone = new AvoidanceZone();
        AvoidanceZoneDto mockDto = mock(AvoidanceZoneDto.class);

        given(avoidanceZoneRepository.calculateMaxExtentMeters(ZONE_GEOM)).willReturn(5_000.0);
        given(avoidanceZoneRepository.sumActiveAreaSquareMeters(USER_ID, null)).willReturn(150 * SQUARE_KM);
        given(avoidanceZoneRepository.calculateAreaSquareMeters(ZONE_GEOM)).willReturn(50 * SQUARE_KM);
        given(userRepository.getReferenceById(USER_ID)).willReturn(mockUser);
        given(avoidanceZoneMapper.toEntity(request, mockUser)).willReturn(mockZone);
        given(avoidanceZoneRepository.save(mockZone)).willReturn(mockZone);
        given(avoidanceZoneMapper.toDto(mockZone)).willReturn(mockDto);

        // When
        AvoidanceZoneDto result = avoidanceZoneService.saveAvoidanceZoneForUser(request, USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockDto);

        then(avoidanceZoneRepository).should().save(mockZone);
    }

    @Test
    void shouldRejectZoneWhenExtentExceedsLimit() {
        // Given
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(ZONE_NAME, ZONE_GEOM);
        given(avoidanceZoneRepository.calculateMaxExtentMeters(ZONE_GEOM)).willReturn(20_001.0);

        // When & Then
        assertThatThrownBy(() -> avoidanceZoneService.saveAvoidanceZoneForUser(request, USER_ID))
                .isInstanceOf(AvoidanceZoneLimitExceededException.class)
                .hasMessageContaining("20 km");

        then(avoidanceZoneRepository).should(never()).save(any());
    }

    @Test
    void shouldRejectZoneWhenTotalActiveAreaExceedsLimit() {
        // Given
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(ZONE_NAME, ZONE_GEOM);
        given(avoidanceZoneRepository.calculateMaxExtentMeters(ZONE_GEOM)).willReturn(5_000.0);
        given(avoidanceZoneRepository.sumActiveAreaSquareMeters(USER_ID, null)).willReturn(150 * SQUARE_KM);
        given(avoidanceZoneRepository.calculateAreaSquareMeters(ZONE_GEOM)).willReturn(60 * SQUARE_KM);

        // When & Then
        assertThatThrownBy(() -> avoidanceZoneService.saveAvoidanceZoneForUser(request, USER_ID))
                .isInstanceOf(AvoidanceZoneLimitExceededException.class)
                .hasMessageContaining("200 km");

        then(avoidanceZoneRepository).should(never()).save(any());
    }

    @Test
    void shouldDeleteAvoidanceZoneSuccessfully() {
        // Given
        AvoidanceZone mockZone = new AvoidanceZone();
        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.of(mockZone));

        // When
        avoidanceZoneService.deleteAvoidanceZone(ZONE_ID, USER_ID);

        // Then
        then(avoidanceZoneRepository).should().delete(mockZone);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentAvoidanceZone() {
        // Given
        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.empty());

        // When & THEN
        assertThatThrownBy(() -> avoidanceZoneService.deleteAvoidanceZone(ZONE_ID, USER_ID))
                .isInstanceOf(AvoidanceZoneNotFoundException.class)
                .hasMessageContaining("Avoidance Zone not found or access denied");

        then(avoidanceZoneRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldToggleAvoidanceActiveStatusSuccessfully() {
        // Given
        AvoidanceZone mockZone = new AvoidanceZone();
        mockZone.setIsActive(false);

        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.of(mockZone));
        given(avoidanceZoneRepository.sumActiveAreaSquareMeters(USER_ID, ZONE_ID)).willReturn(200 * SQUARE_KM);

        // When
        avoidanceZoneService.toggleAvoidanceActiveStatus(ZONE_ID, USER_ID, true);

        // Then
        assertThat(mockZone.getIsActive()).isTrue();
    }

    @Test
    void shouldRejectActivatingZoneWhenTotalActiveAreaExceedsLimit() {
        // Given
        AvoidanceZone mockZone = new AvoidanceZone();
        mockZone.setIsActive(false);
        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.of(mockZone));
        given(avoidanceZoneRepository.sumActiveAreaSquareMeters(USER_ID, ZONE_ID)).willReturn(201 * SQUARE_KM);

        // When & Then
        assertThatThrownBy(() -> avoidanceZoneService.toggleAvoidanceActiveStatus(ZONE_ID, USER_ID, true))
                .isInstanceOf(AvoidanceZoneLimitExceededException.class);

        assertThat(mockZone.getIsActive()).isFalse();
    }

    @Test
    void shouldDeactivateZoneWithoutCheckingAreaLimit() {
        // Given
        AvoidanceZone mockZone = new AvoidanceZone();
        mockZone.setIsActive(true);
        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.of(mockZone));

        // When
        avoidanceZoneService.toggleAvoidanceActiveStatus(ZONE_ID, USER_ID, false);

        // Then
        assertThat(mockZone.getIsActive()).isFalse();
        then(avoidanceZoneRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldThrowExceptionWhenTogglingNonExistentAvoidanceZone() {
        // Given
        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> avoidanceZoneService.toggleAvoidanceActiveStatus(ZONE_ID, USER_ID, true))
                .isInstanceOf(AvoidanceZoneNotFoundException.class)
                .hasMessageContaining("Avoidance Zone not found or access denied");
    }
}
