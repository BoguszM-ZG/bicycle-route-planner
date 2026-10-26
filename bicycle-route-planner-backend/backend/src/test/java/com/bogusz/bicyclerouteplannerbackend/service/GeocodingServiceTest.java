package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.geocoding.GeocodingSearchRequestDto;
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

    private static final String BASE_URL = "https://photon.komoot.io";
    private static final String API_URL_WARSZAWA = "https://photon.komoot.io/api/?q=Warszawa&limit=5";
    private static final String API_URL_KRAKOW = "https://photon.komoot.io/api/?q=Krakow&limit=5&lat=50.06&lon=19.94";

    private static final String QUERY_WARSZAWA = "Warszawa";
    private static final String QUERY_KRAKOW = "Krakow";
    
    private static final Double LAT_KRAKOW = 50.06;
    private static final Double LON_KRAKOW = 19.94;

    private static final String JSON_RESPONSE_WARSZAWA = "{\"features\":[{\"properties\":{\"name\":\"Warszawa\"}}]}";

    private GeocodingService geocodingService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // We create a RestClient Builder to bind our Mock Server to it.
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        // Simulating the configuration from GeocodingConfig.java
        RestClient restClient = builder.baseUrl(BASE_URL).build();

        geocodingService = new GeocodingService(restClient);
    }

    @Test
    void shouldSearchLocationWithoutCoordinates() {
        // Given
        mockServer.expect(requestTo(API_URL_WARSZAWA))
                .andRespond(withSuccess(JSON_RESPONSE_WARSZAWA, MediaType.APPLICATION_JSON));

        // When
        String result = geocodingService.searchLocation(new GeocodingSearchRequestDto(QUERY_WARSZAWA, null, null));

        // Then
        assertThat(result).isEqualTo(JSON_RESPONSE_WARSZAWA);
        mockServer.verify(); // Verifies that the expected request was actually made!
    }

    @Test
    void shouldSearchLocationWithCoordinates() {
        // Given
        mockServer.expect(requestTo(API_URL_KRAKOW))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        // When
        String result = geocodingService.searchLocation(new GeocodingSearchRequestDto(QUERY_KRAKOW, LAT_KRAKOW, LON_KRAKOW));

        // Then
        assertThat(result).isEqualTo("{\"features\":[]}");
        mockServer.verify();
    }

    @Test
    void shouldSearchLocationWithOnlyLatitudeIgnoringCoordinates() {
        // Given
        mockServer.expect(requestTo(API_URL_WARSZAWA))
                .andRespond(withSuccess(JSON_RESPONSE_WARSZAWA, MediaType.APPLICATION_JSON));

        // When
        String result = geocodingService.searchLocation(new GeocodingSearchRequestDto(QUERY_WARSZAWA, LAT_KRAKOW, null));

        // Then
        assertThat(result).isEqualTo(JSON_RESPONSE_WARSZAWA);
        mockServer.verify();
    }

    @Test
    void shouldSearchLocationWithOnlyLongitudeIgnoringCoordinates() {
        // Given
        mockServer.expect(requestTo(API_URL_WARSZAWA))
                .andRespond(withSuccess(JSON_RESPONSE_WARSZAWA, MediaType.APPLICATION_JSON));

        // When
        String result = geocodingService.searchLocation(new GeocodingSearchRequestDto(QUERY_WARSZAWA, null, LON_KRAKOW));

        // Then
        assertThat(result).isEqualTo(JSON_RESPONSE_WARSZAWA);
        mockServer.verify();
    }

    @Test
    void shouldThrowExceptionOnExternalApiError() {
        // Given
        GeocodingSearchRequestDto request = new GeocodingSearchRequestDto("ErrorCity", null, null);
        mockServer.expect(requestTo("https://photon.komoot.io/api/?q=ErrorCity&limit=5"))
                .andRespond(withServerError());

        // When & Then
        assertThatThrownBy(() -> geocodingService.searchLocation(request))
                .isInstanceOf(ExternalGeocodingException.class)
                .hasMessageContaining("Failed to fetch geocoding data");

        mockServer.verify();
    }
}
