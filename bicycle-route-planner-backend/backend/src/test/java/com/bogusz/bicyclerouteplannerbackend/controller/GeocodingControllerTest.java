package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.geocoding.GeocodingSearchRequestDto;
import com.bogusz.bicyclerouteplannerbackend.service.GeocodingService;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeocodingController.class)
@AutoConfigureMockMvc(addFilters = false)
class GeocodingControllerTest {

    private static final String SEARCH_URL = "/api/geocoding/search";
    private static final String PARAM_Q = "q";
    private static final String PARAM_LAT = "lat";
    private static final String PARAM_LON = "lon";
    private static final String QUERY = "Berlin";
    private static final Double LAT = 52.52;
    private static final Double LON = 13.40;
    private static final String EXPECTED_JSON_RESPONSE = "{\"features\": []}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeocodingService geocodingService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturn200AndJsonResponseWhenSearchingLocation() throws Exception {
        // Given
        GeocodingSearchRequestDto request = new GeocodingSearchRequestDto(QUERY, LAT, LON);
        given(geocodingService.searchLocation(request)).willReturn(EXPECTED_JSON_RESPONSE);

        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, QUERY)
                        .param(PARAM_LAT, String.valueOf(LAT))
                        .param(PARAM_LON, String.valueOf(LON)))
                .andExpect(status().isOk())
                .andExpect(content().string(EXPECTED_JSON_RESPONSE));

        // Then (Verify service call)
        then(geocodingService).should(times(1)).searchLocation(request);
    }

    @Test
    void shouldReturn200WhenSearchingLocationWithoutCoordinates() throws Exception {
        // Given
        GeocodingSearchRequestDto request = new GeocodingSearchRequestDto(QUERY, null, null);
        given(geocodingService.searchLocation(request)).willReturn(EXPECTED_JSON_RESPONSE);

        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, QUERY))
                .andExpect(status().isOk())
                .andExpect(content().string(EXPECTED_JSON_RESPONSE));

        then(geocodingService).should(times(1)).searchLocation(request);
    }

    @Test
    void shouldReturn200WhenSearchingLocationWithOnlyLatitude() throws Exception {
        // Given
        GeocodingSearchRequestDto request = new GeocodingSearchRequestDto(QUERY, LAT, null);
        given(geocodingService.searchLocation(request)).willReturn(EXPECTED_JSON_RESPONSE);

        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, QUERY)
                        .param(PARAM_LAT, String.valueOf(LAT)))
                .andExpect(status().isOk())
                .andExpect(content().string(EXPECTED_JSON_RESPONSE));

        then(geocodingService).should(times(1)).searchLocation(request);
    }

    @Test
    void shouldReturn200WhenSearchingLocationWithOnlyLongitude() throws Exception {
        // Given
        GeocodingSearchRequestDto request = new GeocodingSearchRequestDto(QUERY, null, LON);
        given(geocodingService.searchLocation(request)).willReturn(EXPECTED_JSON_RESPONSE);

        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, QUERY)
                        .param(PARAM_LON, String.valueOf(LON)))
                .andExpect(status().isOk())
                .andExpect(content().string(EXPECTED_JSON_RESPONSE));

        then(geocodingService).should(times(1)).searchLocation(request);
    }

    @Test
    void shouldReturn400BadRequestWhenQueryIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(get(SEARCH_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.q").value("Search query is required"));

        then(geocodingService).should(never()).searchLocation(any());
    }

    @Test
    void shouldReturn400BadRequestWhenQueryIsBlank() throws Exception {
        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.q").value("Search query is required"));

        then(geocodingService).should(never()).searchLocation(any());
    }

    @Test
    void shouldReturn400BadRequestWhenQueryExceedsMaxLength() throws Exception {
        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, "a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.q").value("Search query cannot exceed 200 characters"));

        then(geocodingService).should(never()).searchLocation(any());
    }

    @Test
    void shouldReturn400BadRequestWhenLatitudeIsBelowMin() throws Exception {
        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, QUERY)
                        .param(PARAM_LAT, "-90.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.lat").value("Latitude must be between -90.0 and 90.0"));

        then(geocodingService).should(never()).searchLocation(any());
    }

    @Test
    void shouldReturn400BadRequestWhenLatitudeIsAboveMax() throws Exception {
        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, QUERY)
                        .param(PARAM_LAT, "90.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.lat").value("Latitude must be between -90.0 and 90.0"));

        then(geocodingService).should(never()).searchLocation(any());
    }

    @Test
    void shouldReturn400BadRequestWhenLongitudeIsBelowMin() throws Exception {
        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, QUERY)
                        .param(PARAM_LON, "-180.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.lon").value("Longitude must be between -180.0 and 180.0"));

        then(geocodingService).should(never()).searchLocation(any());
    }

    @Test
    void shouldReturn400BadRequestWhenLongitudeIsAboveMax() throws Exception {
        // When & Then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_Q, QUERY)
                        .param(PARAM_LON, "180.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.lon").value("Longitude must be between -180.0 and 180.0"));

        then(geocodingService).should(never()).searchLocation(any());
    }
}
