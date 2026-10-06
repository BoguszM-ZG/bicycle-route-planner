package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
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

import java.util.List;
import java.util.Map;

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
        Map<String, Object> options = Map.of("profile", "cycling-regular");
        OrsRouteRequestDto requestDto = new OrsRouteRequestDto(coordinates, true, true, options);

        given(externalRoutingService.getDirections(any())).willReturn(EXPECTED_GEOJSON_RESPONSE);

        // When
        mockMvc.perform(post("/api/routing/directions")
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
        assertThat(captured.options()).containsEntry("profile", "cycling-regular");
    }
}
