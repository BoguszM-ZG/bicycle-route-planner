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
    private static final String AUTH_REGISTER_URL = "/api/auth/register";
    private static final String ROUTING_DIRECTIONS_URL = "/api/routing/directions";
    private static final String TEST_API_KEY = "dummy-ors-test-key";
    private static final String ORS_API_PATH = "/v2/directions/cycling-road/geojson";
    private static final String MOCK_ROUTE_RESPONSE = "{\"features\":[{\"geometry\":{\"coordinates\":[[19.945,50.0647],[19.946,50.0648]]}}]}";

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
        registry.add("ors.api-url", wireMockServer::baseUrl);
        registry.add("ors.api-key", () -> TEST_API_KEY);
    }

    @BeforeEach
    void setUp() throws Exception {
        userAccessToken = registerUserAndGetAccessToken();
    }

    @Test
    void shouldCalculateRouteSuccessfully() throws Exception {
        // Given
        OrsRouteRequestDto request = new OrsRouteRequestDto(
                List.of(List.of(19.945, 50.0647), List.of(19.946, 50.0648)),
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
                .withRequestBody(containing("19.945")));
    }

    @Test
    void shouldReturn401WhenCalculatingRouteWithoutAuthentication() throws Exception {
        // Given
        OrsRouteRequestDto request = new OrsRouteRequestDto(
                List.of(List.of(19.945, 50.0647), List.of(19.946, 50.0648)),
                false,
                false,
                null
        );

        // When & Then
        mockMvc.perform(post(ROUTING_DIRECTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(ORS_API_PATH)));
    }

    @Test
    void shouldHandleExternalApiException() throws Exception {
        // Given
        OrsRouteRequestDto request = new OrsRouteRequestDto(
                List.of(List.of(19.945, 50.0647), List.of(19.946, 50.0648)),
                false,
                false,
                null
        );

        wireMockServer.stubFor(WireMock.post(urlPathEqualTo(ORS_API_PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(TEST_API_KEY))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\":{\"code\":2004,\"message\":\"Request parameters exceed limits\"}}")));

        // When & Then
        mockMvc.perform(post(ROUTING_DIRECTIONS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway());
    }

    private String registerUserAndGetAccessToken() throws Exception {
        RegisterRequest request = new RegisterRequest(TEST_USER_EMAIL, TEST_USER_PASSWORD, "Routing Tester");
        String responseContent = mockMvc.perform(post(AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, AuthResponse.class).accessToken();
    }
}
