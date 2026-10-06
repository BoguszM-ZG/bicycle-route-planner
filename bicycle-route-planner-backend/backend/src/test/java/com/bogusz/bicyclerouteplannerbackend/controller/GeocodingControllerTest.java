package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.service.GeocodingService;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeocodingController.class)
@AutoConfigureMockMvc(addFilters = false)
class GeocodingControllerTest {

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
        given(geocodingService.searchLocation(QUERY, LAT, LON)).willReturn(EXPECTED_JSON_RESPONSE);

        // When & Then
        mockMvc.perform(get("/api/geocoding/search")
                        .param("q", QUERY)
                        .param("lat", LAT.toString())
                        .param("lon", LON.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(EXPECTED_JSON_RESPONSE));

        // Then (Verify service call)
        then(geocodingService).should(times(1)).searchLocation(QUERY, LAT, LON);
    }
}
