package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.ExternalRoutingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalRoutingServiceTest {

    private static final String BASE_URL = "https://api.openrouteservice.org";
    private static final String API_URL = "https://api.openrouteservice.org/v2/directions/cycling-road/geojson";
    private static final String JSON_RESPONSE_SUCCESS = "{\"routes\":[{\"geometry\":\"encoded_polyline\"}]}";

    private ExternalRoutingService externalRoutingService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.baseUrl(BASE_URL).build();
        externalRoutingService = new ExternalRoutingService(restClient);
    }

    @Test
    void shouldGetDirectionsSuccessfully() {
        // GIVEN
        OrsRouteRequestDto request = new OrsRouteRequestDto(List.of(), true, false, null);

        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"coordinates\":[],\"elevation\":true,\"instructions\":false}"))
                .andRespond(withSuccess(JSON_RESPONSE_SUCCESS, MediaType.APPLICATION_JSON));

        // WHEN
        String result = externalRoutingService.getDirections(request);

        // THEN
        assertThat(result).isEqualTo(JSON_RESPONSE_SUCCESS);
        mockServer.verify();
    }

    @Test
    void shouldThrowExceptionOnApiError() {
        // GIVEN
        OrsRouteRequestDto request = new OrsRouteRequestDto(List.of(), true, false, null);

        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"coordinates\":[],\"elevation\":true,\"instructions\":false}"))
                .andRespond(withServerError());

        // WHEN & THEN
        assertThatThrownBy(() -> externalRoutingService.getDirections(request))
                .isInstanceOf(ExternalRoutingException.class)
                .hasMessageContaining("Failed to calculate route");

        mockServer.verify();
    }
}
