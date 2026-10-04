package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.CreateAvoidanceZoneRequestDto;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AvoidanceZoneServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ZONE_ID = 100L;
    private static final String ZONE_NAME = "Construction Site";
    private static final String ZONE_GEOM = "POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))";

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
        // GIVEN
        AvoidanceZone mockZone = new AvoidanceZone();
        AvoidanceZoneDto mockDto = mock(AvoidanceZoneDto.class);

        given(avoidanceZoneRepository.findByUserId(USER_ID)).willReturn(List.of(mockZone));
        given(avoidanceZoneMapper.toDto(mockZone)).willReturn(mockDto);

        // WHEN
        List<AvoidanceZoneDto> result = avoidanceZoneService.getAvoidanceZonesForUser(USER_ID);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(mockDto);
    }


    @Test
    void shouldSaveAvoidanceZoneForUserSuccessfully() {
        // GIVEN
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(ZONE_NAME, ZONE_GEOM);
        User mockUser = new User();
        AvoidanceZone mockZone = new AvoidanceZone();
        AvoidanceZoneDto mockDto = mock(AvoidanceZoneDto.class);

        given(userRepository.getReferenceById(USER_ID)).willReturn(mockUser);
        given(avoidanceZoneMapper.toEntity(request, mockUser)).willReturn(mockZone);
        given(avoidanceZoneRepository.save(mockZone)).willReturn(mockZone);
        given(avoidanceZoneMapper.toDto(mockZone)).willReturn(mockDto);

        // WHEN
        AvoidanceZoneDto result = avoidanceZoneService.saveAvoidanceZoneForUser(request, USER_ID);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockDto);

        then(avoidanceZoneRepository).should().save(mockZone);
    }


    @Test
    void shouldDeleteAvoidanceZoneSuccessfully() {
        // GIVEN
        AvoidanceZone mockZone = new AvoidanceZone();
        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.of(mockZone));

        // WHEN
        avoidanceZoneService.deleteAvoidanceZone(ZONE_ID, USER_ID);

        // THEN
        then(avoidanceZoneRepository).should().delete(mockZone);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentAvoidanceZone() {
        // GIVEN
        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> avoidanceZoneService.deleteAvoidanceZone(ZONE_ID, USER_ID))
                .isInstanceOf(AvoidanceZoneNotFoundException.class)
                .hasMessageContaining("Avoidance Zone not found or access denied");

        then(avoidanceZoneRepository).shouldHaveNoMoreInteractions();
    }


    @Test
    void shouldToggleAvoidanceActiveStatusSuccessfully() {
        // GIVEN
        AvoidanceZone mockZone = new AvoidanceZone();
        mockZone.setIsActive(false);

        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.of(mockZone));

        // WHEN
        avoidanceZoneService.toggleAvoidanceActiveStatus(ZONE_ID, USER_ID, true);

        // THEN
        // Verifying Hibernate's Dirty Checking mechanism
        assertThat(mockZone.getIsActive()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenTogglingNonExistentAvoidanceZone() {
        // GIVEN
        given(avoidanceZoneRepository.findByIdAndUserId(ZONE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> avoidanceZoneService.toggleAvoidanceActiveStatus(ZONE_ID, USER_ID, true))
                .isInstanceOf(AvoidanceZoneNotFoundException.class)
                .hasMessageContaining("Avoidance Zone not found or access denied");
    }
}