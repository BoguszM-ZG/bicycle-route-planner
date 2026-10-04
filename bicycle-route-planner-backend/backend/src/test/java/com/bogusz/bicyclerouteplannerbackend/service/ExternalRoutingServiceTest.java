package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.ExternalRoutingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalRoutingServiceTest {

    private ExternalRoutingService externalRoutingService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.baseUrl("https://api.openrouteservice.org").build();
        externalRoutingService = new ExternalRoutingService(restClient);
    }


    @Test
    void shouldGetDirectionsSuccessfully() {
        // GIVEN
        OrsRouteRequestDto request = new OrsRouteRequestDto(List.of(), true, false, null);
        String expectedJsonResponse = "{\"routes\":[{\"geometry\":\"encoded_polyline\"}]}";
        String expectedUrl = "https://api.openrouteservice.org/v2/directions/cycling-road/geojson";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(expectedJsonResponse, MediaType.APPLICATION_JSON));

        // WHEN
        String result = externalRoutingService.getDirections(request);

        // THEN
        assertThat(result).isEqualTo(expectedJsonResponse);
        mockServer.verify();
    }


    @Test
    void shouldThrowExceptionOnApiError() {
        // GIVEN
        OrsRouteRequestDto request = new OrsRouteRequestDto(List.of(), true, false, null);
        String expectedUrl = "https://api.openrouteservice.org/v2/directions/cycling-road/geojson";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withServerError()); // Simulating HTTP 500 from ORS

        // WHEN & THEN
        assertThatThrownBy(() -> externalRoutingService.getDirections(request))
                .isInstanceOf(ExternalRoutingException.class)
                .hasMessageContaining("Failed to calculate route");

        mockServer.verify();
    }
}