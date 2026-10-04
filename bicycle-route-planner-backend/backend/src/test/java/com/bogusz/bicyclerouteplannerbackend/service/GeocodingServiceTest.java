package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.ExternalGeocodingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeocodingServiceTest {

    private GeocodingService geocodingService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // We create a RestClient Builder to bind our Mock Server to it.
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        // Simulating the configuration from GeocodingConfig.java
        RestClient restClient = builder.baseUrl("https://photon.komoot.io").build();

        geocodingService = new GeocodingService(restClient);
    }


    @Test
    void shouldSearchLocationWithoutCoordinates() {
        // GIVEN
        String expectedJsonResponse = "{\"features\":[{\"properties\":{\"name\":\"Warszawa\"}}]}";
        String expectedUrl = "https://photon.komoot.io/api/?q=Warszawa&limit=5";

        // We instruct our fake server to expect a specific URL and return a prepared JSON
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(expectedJsonResponse, MediaType.APPLICATION_JSON));

        // WHEN
        String result = geocodingService.searchLocation("Warszawa", null, null);

        // THEN
        assertThat(result).isEqualTo(expectedJsonResponse);
        mockServer.verify(); // Verifies that the expected request was actually made!
    }

    @Test
    void shouldSearchLocationWithCoordinates() {
        // GIVEN
        String expectedJsonResponse = "{\"features\":[]}";
        String expectedUrl = "https://photon.komoot.io/api/?q=Krakow&limit=5&lat=50.06&lon=19.94";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(expectedJsonResponse, MediaType.APPLICATION_JSON));

        // WHEN
        String result = geocodingService.searchLocation("Krakow", 50.06, 19.94);

        // THEN
        assertThat(result).isEqualTo(expectedJsonResponse);
        mockServer.verify();
    }


    @Test
    void shouldThrowExceptionOnExternalApiError() {
        // GIVEN
        String expectedUrl = "https://photon.komoot.io/api/?q=ErrorCity&limit=5";

        // We instruct our fake server to simulate an HTTP 500 Internal Server Error
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withServerError());

        // WHEN & THEN
        assertThatThrownBy(() -> geocodingService.searchLocation("ErrorCity", null, null))
                .isInstanceOf(ExternalGeocodingException.class)
                .hasMessageContaining("Failed to fetch geocoding data");

        mockServer.verify();
    }
}