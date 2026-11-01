package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.AvoidPolygonsDto;
import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsOptionsDto;
import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.routing.RoundTripDto;
import com.bogusz.bicyclerouteplannerbackend.exception.ExternalRoutingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
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
    void shouldSendRoundTripOptionsInOrsFormatWithoutNullFields() {
        // GIVEN
        OrsRouteRequestDto request = new OrsRouteRequestDto(
                List.of(List.of(19.0, 52.0)), true, false,
                new OrsOptionsDto(null, new RoundTripDto(10000, null)));

        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        "{\"coordinates\":[[19.0,52.0]],\"elevation\":true,\"instructions\":false,"
                                + "\"options\":{\"round_trip\":{\"length\":10000}}}",
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess(JSON_RESPONSE_SUCCESS, MediaType.APPLICATION_JSON));

        // WHEN
        externalRoutingService.getDirections(request);

        // THEN
        mockServer.verify();
    }

    @Test
    void shouldSendAvoidPolygonsAsGeoJsonMultiPolygon() {
        // GIVEN
        List<List<List<List<Double>>>> polygons = List.of(List.of(List.of(
                List.of(19.0, 52.0), List.of(19.1, 52.0), List.of(19.1, 52.1), List.of(19.0, 52.0))));
        OrsRouteRequestDto request = new OrsRouteRequestDto(
                List.of(List.of(19.0, 52.0), List.of(19.2, 52.2)), true, false,
                new OrsOptionsDto(new AvoidPolygonsDto(polygons), null));

        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        "{\"coordinates\":[[19.0,52.0],[19.2,52.2]],\"elevation\":true,\"instructions\":false,"
                                + "\"options\":{\"avoid_polygons\":{\"type\":\"MultiPolygon\",\"coordinates\":"
                                + "[[[[19.0,52.0],[19.1,52.0],[19.1,52.1],[19.0,52.0]]]]}}}",
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess(JSON_RESPONSE_SUCCESS, MediaType.APPLICATION_JSON));

        // WHEN
        externalRoutingService.getDirections(request);

        // THEN
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
                .hasMessage("HTTP 500");

        mockServer.verify();
    }

    @Test
    void shouldIncludeOrsErrorCodeButNotMessageInException() {
        // GIVEN
        OrsRouteRequestDto request = new OrsRouteRequestDto(List.of(List.of(19.0, 52.0), List.of(19.1, 52.1)), true, false, null);
        String orsErrorBody = "{\"error\":{\"code\":2010,\"message\":\"Could not find routable point near coordinate 0: 19.0 52.0\"}}";

        mockServer.expect(requestTo(API_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(orsErrorBody));

        // WHEN & THEN
        assertThatThrownBy(() -> externalRoutingService.getDirections(request))
                .isInstanceOf(ExternalRoutingException.class)
                .hasMessage("HTTP 404, ORS code 2010");

        mockServer.verify();
    }

    @Test
    void shouldReportOnlyStatusWhenOrsErrorBodyIsNotJson() {
        // GIVEN
        OrsRouteRequestDto request = new OrsRouteRequestDto(List.of(List.of(19.0, 52.0), List.of(19.1, 52.1)), true, false, null);

        mockServer.expect(requestTo(API_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html>Service Unavailable</html>"));

        // WHEN & THEN
        assertThatThrownBy(() -> externalRoutingService.getDirections(request))
                .isInstanceOf(ExternalRoutingException.class)
                .hasMessage("HTTP 503");

        mockServer.verify();
    }
}
