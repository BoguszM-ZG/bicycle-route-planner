package com.bogusz.bicyclerouteplannerbackend.integration;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.fasterxml.jackson.databind.json.JsonMapper;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class GeocodingIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_USER_EMAIL = "geocoding.integration@example.com";
    private static final String TEST_USER_PASSWORD = "HardPass123!";
    private static final String TEST_USER_FULL_NAME = "Geo User";
    
    private static final String AUTH_REGISTER_URL = "/api/auth/register";
    private static final String GEOCODING_SEARCH_URL = "/api/geocoding/search";
    
    private static final String PHOTON_API_URL_PROPERTY = "photon.api.url";
    private static final String PHOTON_API_PATH = "/api/";
    
    private static final String PARAM_Q = "q";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_LAT = "lat";
    private static final String PARAM_LON = "lon";
    
    private static final String DEFAULT_LIMIT_VALUE = "5";
    
    private static final String CITY_WARSZAWA = "Warszawa";
    private static final String CITY_KRAKOW = "Krakow";
    private static final String CITY_ERROR = "ErrorCity";
    
    private static final String MOCK_RESPONSE_WARSZAWA = "{\"features\":[{\"properties\":{\"name\":\"Warszawa\"}}]}";
    private static final String MOCK_RESPONSE_KRAKOW = "{\"features\":[{\"properties\":{\"name\":\"Krakow\"}}]}";
    private static final String MOCK_ERROR_RESPONSE = "Internal Server Error from Photon";
    
    private static final String LAT_KRAKOW = "50.0647";
    private static final String LON_KRAKOW_INPUT = "19.9450";
    private static final String LON_KRAKOW_EXPECTED = "19.945";

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
        registry.add(PHOTON_API_URL_PROPERTY, wireMockServer::baseUrl);
    }

    @BeforeEach
    void setUp() throws Exception {
        userAccessToken = registerUserAndGetAccessToken();
    }

    @Test
    void shouldReturnGeocodingResultsSuccessfully() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo(PHOTON_API_PATH))
                .withQueryParam(PARAM_Q, equalTo(CITY_WARSZAWA))
                .withQueryParam(PARAM_LIMIT, equalTo(DEFAULT_LIMIT_VALUE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(MOCK_RESPONSE_WARSZAWA)));

        // When & Then
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param(PARAM_Q, CITY_WARSZAWA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(content().json(MOCK_RESPONSE_WARSZAWA));
    }

    @Test
    void shouldReturnGeocodingResultsWithLatLon() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo(PHOTON_API_PATH))
                .withQueryParam(PARAM_Q, equalTo(CITY_KRAKOW))
                .withQueryParam(PARAM_LIMIT, equalTo(DEFAULT_LIMIT_VALUE))
                .withQueryParam(PARAM_LAT, equalTo(LAT_KRAKOW))
                .withQueryParam(PARAM_LON, equalTo(LON_KRAKOW_EXPECTED))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(MOCK_RESPONSE_KRAKOW)));

        // When & Then
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param(PARAM_Q, CITY_KRAKOW)
                        .param(PARAM_LAT, LAT_KRAKOW)
                        .param(PARAM_LON, LON_KRAKOW_INPUT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(content().json(MOCK_RESPONSE_KRAKOW));
    }

    @Test
    void shouldHandleExternalApiException() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo(PHOTON_API_PATH))
                .withQueryParam(PARAM_Q, equalTo(CITY_ERROR))
                .withQueryParam(PARAM_LIMIT, equalTo(DEFAULT_LIMIT_VALUE))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody(MOCK_ERROR_RESPONSE)));

        // When & Then
        mockMvc.perform(get(GEOCODING_SEARCH_URL)
                        .param(PARAM_Q, CITY_ERROR)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
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

