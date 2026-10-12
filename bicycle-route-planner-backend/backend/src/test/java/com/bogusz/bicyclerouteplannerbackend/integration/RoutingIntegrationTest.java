package com.bogusz.bicyclerouteplannerbackend.integration;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class RoutingIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_USER_EMAIL = "routing.integration@example.com";
    private static final String TEST_USER_PASSWORD = "StrongPassword123!";
    private static final String TEST_USER_FULL_NAME = "Routing Tester";

    private static final String AUTH_REGISTER_URL = "/api/auth/register";
    private static final String ROUTING_DIRECTIONS_URL = "/api/routing/directions";

    private static final String ORS_API_URL_PROPERTY = "ors.api-url";
    private static final String ORS_API_KEY_PROPERTY = "ors.api-key";
    
    private static final double ROUTE_START_LON = 19.945;
    private static final double ROUTE_START_LAT = 50.0647;
    private static final double ROUTE_END_LON = 19.946;
    private static final double ROUTE_END_LAT = 50.0648;

    private static final String TEST_API_KEY = "dummy-ors-test-key";
    private static final String ORS_API_PATH = "/v2/directions/cycling-road/geojson";

    private static final String MOCK_ROUTE_RESPONSE = "{\"features\":[{\"geometry\":{\"coordinates\":[[" + ROUTE_START_LON + "," + ROUTE_START_LAT + "],[" + ROUTE_END_LON + "," + ROUTE_END_LAT + "]]}}]}";
    private static final String MOCK_ERROR_RESPONSE = "{\"error\":{\"code\":2004,\"message\":\"Request parameters exceed limits\"}}";

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
    private String userAccessToken;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(ORS_API_URL_PROPERTY, wireMockServer::baseUrl);
        registry.add(ORS_API_KEY_PROPERTY, () -> TEST_API_KEY);
    }

    @BeforeEach
    void setUp() throws Exception {
        userAccessToken = registerUserAndGetAccessToken();
    }


    @Test
    void shouldCalculateRouteSuccessfully() throws Exception {
        // Given
        OrsRouteRequestDto request = new OrsRouteRequestDto(
                List.of(List.of(ROUTE_START_LON, ROUTE_START_LAT), List.of(ROUTE_END_LON, ROUTE_END_LAT)),
                false,
                false,
                null
        );

        wireMockServer.stubFor(WireMock.post(urlPathEqualTo(ORS_API_PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(TEST_API_KEY))
                .withRequestBody(matchingJsonPath("$.coordinates"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(MOCK_ROUTE_RESPONSE)));

        // When & Then
        mockMvc.perform(post(ROUTING_DIRECTIONS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(MOCK_ROUTE_RESPONSE));
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(ORS_API_PATH))
                .withRequestBody(containing(String.valueOf(ROUTE_START_LON))));
    }

    @Test
    void shouldHandleExternalApiException() throws Exception {
        // Given
        OrsRouteRequestDto request = new OrsRouteRequestDto(
                List.of(List.of(ROUTE_START_LON, ROUTE_START_LAT), List.of(ROUTE_END_LON, ROUTE_END_LAT)),
                false,
                false,
                null
        );

        wireMockServer.stubFor(WireMock.post(urlPathEqualTo(ORS_API_PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(TEST_API_KEY))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody(MOCK_ERROR_RESPONSE)));

        // When & Then
        mockMvc.perform(post(ROUTING_DIRECTIONS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway());
    }

    private String registerUserAndGetAccessToken() throws Exception {
        RegisterRequest request = new RegisterRequest(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_FULL_NAME);
        String responseContent = mockMvc.perform(post(AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, AuthResponse.class).accessToken();
    }
}
