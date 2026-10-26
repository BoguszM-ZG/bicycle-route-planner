package com.bogusz.bicyclerouteplannerbackend.integration;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class GeocodingIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_USER_EMAIL = "geocoding.integration@example.com";
    private static final String TEST_USER_PASSWORD = "HardPass123!";
    private static final String AUTH_REGISTER_URL = "/api/auth/register";
    private static final String GEOCODING_SEARCH_URL = "/api/geocoding/search";
    private static final String PHOTON_API_PATH = "/api/";
    private static final String CITY_WARSZAWA = "Warszawa";
    private static final String MOCK_RESPONSE_WARSZAWA = "{\"features\":[{\"properties\":{\"name\":\"Warszawa\"}}]}";

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
        registry.add("photon.api.url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void setUp() throws Exception {
        userAccessToken = registerUserAndGetAccessToken();
    }

    @Test
    void shouldReturnGeocodingResultsSuccessfully() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo(PHOTON_API_PATH))
                .withQueryParam("q", equalTo(CITY_WARSZAWA))
                .withQueryParam("limit", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(MOCK_RESPONSE_WARSZAWA)));

        // When & Then
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param("q", CITY_WARSZAWA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(content().json(MOCK_RESPONSE_WARSZAWA));
    }

    @Test
    void shouldReturnGeocodingResultsWithLatLon() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo(PHOTON_API_PATH))
                .withQueryParam("q", equalTo("Krakow"))
                .withQueryParam("limit", equalTo("5"))
                .withQueryParam("lat", equalTo("50.0647"))
                .withQueryParam("lon", equalTo("19.945"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"features\":[{\"properties\":{\"name\":\"Krakow\"}}]}")));

        // When & Then
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param("q", "Krakow")
                        .param("lat", "50.0647")
                        .param("lon", "19.9450")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"features\":[{\"properties\":{\"name\":\"Krakow\"}}]}"));
    }

    @Test
    void shouldReturnGeocodingResultsWhenOnlyLatitudeProvided() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo(PHOTON_API_PATH))
                .withQueryParam("q", equalTo(CITY_WARSZAWA))
                .withQueryParam("limit", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(MOCK_RESPONSE_WARSZAWA)));

        // When & Then
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param("q", CITY_WARSZAWA)
                        .param("lat", "50.0647")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(content().json(MOCK_RESPONSE_WARSZAWA));
    }

    @Test
    void shouldReturnGeocodingResultsWhenOnlyLongitudeProvided() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo(PHOTON_API_PATH))
                .withQueryParam("q", equalTo(CITY_WARSZAWA))
                .withQueryParam("limit", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(MOCK_RESPONSE_WARSZAWA)));

        // When & Then
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param("q", CITY_WARSZAWA)
                        .param("lon", "19.9450")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(content().json(MOCK_RESPONSE_WARSZAWA));
    }

    @Test
    void shouldHandleExternalApiException() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo(PHOTON_API_PATH))
                .withQueryParam("q", equalTo("ErrorCity"))
                .withQueryParam("limit", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error from Photon")));

        // When & Then
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param("q", "ErrorCity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isBadGateway());
    }

    @Test
    void shouldReturn400WhenSearchingWithBlankQuery() throws Exception {
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param("q", "   ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.q").value("Search query is required"));
    }

    private String registerUserAndGetAccessToken() throws Exception {
        RegisterRequest request = new RegisterRequest(TEST_USER_EMAIL, TEST_USER_PASSWORD, "Geo User");
        String responseContent = mockMvc.perform(post(AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, AuthResponse.class).accessToken();
    }
}

