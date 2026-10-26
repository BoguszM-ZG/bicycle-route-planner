package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.CreateAvoidanceZoneRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.UpdateAvoidanceZoneStatusDto;
import com.bogusz.bicyclerouteplannerbackend.service.AvoidanceZoneService;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AvoidanceZoneController.class)
@AutoConfigureMockMvc(addFilters = false)
class AvoidanceZoneControllerTest {

    private static final long USER_ID = 99L;
    private static final long ZONE_ID = 5L;
    private static final String ZONE_NAME = "Construction Site";
    private static final String ZONE_GEOM = "POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))";
    private static final String AVOIDANCE_ZONES_URL = "/api/avoidance-zones";
    private static final String USER_EMAIL = "test@test.com";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AvoidanceZoneService avoidanceZoneService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(getMockAuthentication());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturn200AndAvoidanceZoneListWhenGettingUserZones() throws Exception {
        // Given
        AvoidanceZoneDto zoneDto = AvoidanceZoneDto.builder()
                .id(ZONE_ID)
                .name(ZONE_NAME)
                .zoneGeom(ZONE_GEOM)
                .isActive(true)
                .build();
        given(avoidanceZoneService.getAvoidanceZonesForUser(USER_ID)).willReturn(List.of(zoneDto));

        // When & Then
        mockMvc.perform(get(AVOIDANCE_ZONES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(ZONE_ID))
                .andExpect(jsonPath("$[0].name").value(ZONE_NAME))
                .andExpect(jsonPath("$[0].zoneGeom").value(ZONE_GEOM))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void shouldReturn201WhenCreatingValidAvoidanceZone() throws Exception {
        // Given
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(ZONE_NAME, ZONE_GEOM);
        AvoidanceZoneDto response = AvoidanceZoneDto.builder()
                .id(ZONE_ID)
                .name(ZONE_NAME)
                .zoneGeom(ZONE_GEOM)
                .isActive(true)
                .build();

        given(avoidanceZoneService.saveAvoidanceZoneForUser(any(), eq(USER_ID))).willReturn(response);

        // When
        mockMvc.perform(post(AVOIDANCE_ZONES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ZONE_ID))
                .andExpect(jsonPath("$.name").value(ZONE_NAME));

        // Then
        ArgumentCaptor<CreateAvoidanceZoneRequestDto> createRequestCaptor = ArgumentCaptor.forClass(CreateAvoidanceZoneRequestDto.class);
        then(avoidanceZoneService).should(times(1)).saveAvoidanceZoneForUser(createRequestCaptor.capture(), eq(USER_ID));
        
        CreateAvoidanceZoneRequestDto capturedRequest = createRequestCaptor.getValue();
        assertThat(capturedRequest.name()).isEqualTo(ZONE_NAME);
        assertThat(capturedRequest.zoneGeom()).isEqualTo(ZONE_GEOM);
    }

    @Test
    void shouldReturn400BadRequestWhenCreatingZoneWithBlankName() throws Exception {
        // Given
        CreateAvoidanceZoneRequestDto badRequest = new CreateAvoidanceZoneRequestDto("", ZONE_GEOM);

        // When & Then
        mockMvc.perform(post(AVOIDANCE_ZONES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(avoidanceZoneService).should(never()).saveAvoidanceZoneForUser(any(), any());
    }

    @Test
    void shouldReturn400BadRequestWhenZoneGeometryIsInvalid() throws Exception {
        // Given
        CreateAvoidanceZoneRequestDto badRequest = new CreateAvoidanceZoneRequestDto(
                ZONE_NAME, "POLYGON((0 0, 0 2, 2 0, 2 2, 0 0))");

        // When & Then
        mockMvc.perform(post(AVOIDANCE_ZONES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        then(avoidanceZoneService).should(never()).saveAvoidanceZoneForUser(any(), any());
    }

    @Test
    void shouldReturn204WhenDeletingAvoidanceZone() throws Exception {
        // When & Then
        mockMvc.perform(delete(AVOIDANCE_ZONES_URL + "/" + ZONE_ID))
                .andExpect(status().isNoContent());

        // Then
        then(avoidanceZoneService).should(times(1)).deleteAvoidanceZone(ZONE_ID, USER_ID);
    }

    @Test
    void shouldReturn204WhenUpdatingAvoidanceZoneStatus() throws Exception {
        // Given
        UpdateAvoidanceZoneStatusDto request = new UpdateAvoidanceZoneStatusDto(false);

        // When
        mockMvc.perform(patch(AVOIDANCE_ZONES_URL + "/" + ZONE_ID + "/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then
        then(avoidanceZoneService).should(times(1)).toggleAvoidanceActiveStatus(ZONE_ID, USER_ID, false);
    }

    @Test
    void shouldReturn400BadRequestWhenUpdatingStatusWithNull() throws Exception {
        // Given
        UpdateAvoidanceZoneStatusDto badRequest = new UpdateAvoidanceZoneStatusDto(null);

        // When & Then
        mockMvc.perform(patch(AVOIDANCE_ZONES_URL + "/" + ZONE_ID + "/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(avoidanceZoneService).should(never()).toggleAvoidanceActiveStatus(anyLong(), anyLong(), anyBoolean());
    }

    private Authentication getMockAuthentication() {
        UserPrincipal principal = UserPrincipal.of(USER_ID, USER_EMAIL);
        return new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
    }
}
