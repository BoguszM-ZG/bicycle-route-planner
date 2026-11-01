package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsOptionsDto;
import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.routing.RoundTripDto;
import com.bogusz.bicyclerouteplannerbackend.service.ExternalRoutingService;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoutingController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoutingControllerTest {

    private static final String DIRECTIONS_URL = "/api/routing/directions";
    private static final String EXPECTED_GEOJSON_RESPONSE = "{\"type\": \"FeatureCollection\"}";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ExternalRoutingService externalRoutingService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturn200AndRouteResponseWhenGettingDirections() throws Exception {
        // Given
        List<List<Double>> coordinates = List.of(List.of(13.4, 52.5), List.of(13.5, 52.6));
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(coordinates, true, true, null);

        given(externalRoutingService.getDirections(any())).willReturn(EXPECTED_GEOJSON_RESPONSE);

        // When
        mockMvc.perform(post(DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string(EXPECTED_GEOJSON_RESPONSE));

        // Then
        ArgumentCaptor<OrsRouteRequestDto> captor = ArgumentCaptor.forClass(OrsRouteRequestDto.class);
        then(externalRoutingService).should(times(1)).getDirections(captor.capture());

        OrsRouteRequestDto captured = captor.getValue();
        assertThat(captured.elevation()).isTrue();
        assertThat(captured.instructions()).isTrue();
        assertThat(captured.coordinates()).hasSize(2);
    }

    @Test
    void shouldReturn200WhenOneCoordinateProvidedWithRoundTrip() throws Exception {
        // Given
        List<List<Double>> coordinates = List.of(List.of(13.4, 52.5));
        OrsOptionsDto options = new OrsOptionsDto(null, new RoundTripDto(10000, null));
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(coordinates, true, false, options);

        given(externalRoutingService.getDirections(any())).willReturn(EXPECTED_GEOJSON_RESPONSE);

        // When & Then
        mockMvc.perform(post(DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string(EXPECTED_GEOJSON_RESPONSE));
    }

    @Test
    void shouldIgnoreUnknownOptionsAndPassOnlyKnownOnes() throws Exception {
        // Given
        String requestJson = """
                {
                  "coordinates": [[13.4, 52.5]],
                  "elevation": true,
                  "instructions": false,
                  "options": {
                    "round_trip": { "length": 10000, "seed": 5 },
                    "alternative_routes": { "target_count": 3 }
                  }
                }
                """;

        given(externalRoutingService.getDirections(any())).willReturn(EXPECTED_GEOJSON_RESPONSE);

        // When
        mockMvc.perform(post(DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then
        ArgumentCaptor<OrsRouteRequestDto> captor = ArgumentCaptor.forClass(OrsRouteRequestDto.class);
        then(externalRoutingService).should(times(1)).getDirections(captor.capture());

        OrsRouteRequestDto captured = captor.getValue();
        assertThat(captured.options()).isEqualTo(new OrsOptionsDto(null, new RoundTripDto(10000, 5)));
        assertThat(objectMapper.writeValueAsString(captured)).doesNotContain("alternative_routes");
    }

    @Test
    void shouldReturn400WhenMoreThan50CoordinatesProvided() throws Exception {
        // Given
        List<List<Double>> coordinates = Collections.nCopies(51, List.of(13.4, 52.5));
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(coordinates, true, true, null);

        // When & Then
        mockMvc.perform(post(DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
        then(externalRoutingService).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturn400WhenRoundTripLengthExceeds100Km() throws Exception {
        // Given
        OrsOptionsDto options = new OrsOptionsDto(null, new RoundTripDto(100_001, null));
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(List.of(List.of(13.4, 52.5)), true, false, options);

        // When & Then
        mockMvc.perform(post(DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
        then(externalRoutingService).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturn400WhenRoundTripHasMoreThanOneCoordinate() throws Exception {
        // Given
        OrsOptionsDto options = new OrsOptionsDto(null, new RoundTripDto(10000, null));
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(
                List.of(List.of(13.4, 52.5), List.of(13.5, 52.6)), true, false, options);

        // When & Then
        mockMvc.perform(post(DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
        then(externalRoutingService).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturn400WhenCoordinatesAreEmpty() throws Exception {
        // Given
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(List.of(), true, true, null);

        // When & Then
        mockMvc.perform(post(DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenOnlyOneCoordinateProvidedWithoutRoundTrip() throws Exception {
        // Given
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(List.of(List.of(13.4, 52.5)), true, true, null);

        // When & Then
        mockMvc.perform(post(DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSerializeCleanJsonWithoutExtraFields() throws Exception {
        // Given
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(List.of(List.of(13.4, 52.5), List.of(13.5, 52.6)), true, true, null);

        // When
        String json = objectMapper.writeValueAsString(requestDto);

        // Then
        assertThat(json).doesNotContain("validCoordinateCount");
        assertThat(json).contains("\"coordinates\"");
    }
}
