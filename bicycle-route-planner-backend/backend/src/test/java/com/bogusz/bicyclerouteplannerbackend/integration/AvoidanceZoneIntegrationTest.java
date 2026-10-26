package com.bogusz.bicyclerouteplannerbackend.integration;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.AvoidanceZoneDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.CreateAvoidanceZoneRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.avoidance_zone.UpdateAvoidanceZoneStatusDto;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class AvoidanceZoneIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_USER_EMAIL = "avoidance.integration@example.com";
    private static final String TEST_USER_PASSWORD = "StrongPassword123!";
    private static final String AUTH_REGISTER_URL = "/api/auth/register";
    private static final String AVOIDANCE_ZONES_URL = "/api/avoidance-zones";
    private static final String TEST_ZONE_NAME = "Construction Site";
    private static final String VALID_WKT_POLYGON = "POLYGON ((19.945 50.064, 19.946 50.064, 19.946 50.065, 19.945 50.065, 19.945 50.064))";

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
    private String userAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        userAccessToken = registerUserAndGetAccessToken(TEST_USER_EMAIL, TEST_USER_PASSWORD, "Avoidance Tester");
    }

    @Test
    void shouldCreateAndRetrieveAvoidanceZone() throws Exception {
        // Given
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(TEST_ZONE_NAME, VALID_WKT_POLYGON);

        // When
        mockMvc.perform(post(AVOIDANCE_ZONES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(TEST_ZONE_NAME))
                .andExpect(jsonPath("$.zoneGeom").value(VALID_WKT_POLYGON))
                .andExpect(jsonPath("$.isActive").value(true));

        // Then
        mockMvc.perform(get(AVOIDANCE_ZONES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(TEST_ZONE_NAME));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingZoneWithInvalidGeometry() throws Exception {
        // Given
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(TEST_ZONE_NAME, "POLYGON (NOT A VALID GEOM)");

        // When & Then
        mockMvc.perform(post(AVOIDANCE_ZONES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldToggleAvoidanceZoneStatus() throws Exception {
        // Given
        Long zoneId = createDefaultAvoidanceZone(userAccessToken);
        UpdateAvoidanceZoneStatusDto updateRequest = new UpdateAvoidanceZoneStatusDto(false);

        // When
        mockMvc.perform(patch(AVOIDANCE_ZONES_URL + "/" + zoneId + "/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get(AVOIDANCE_ZONES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(false));
    }

    @Test
    void shouldDeleteAvoidanceZone() throws Exception {
        // Given
        Long zoneId = createDefaultAvoidanceZone(userAccessToken);

        // When
        mockMvc.perform(delete(AVOIDANCE_ZONES_URL + "/" + zoneId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get(AVOIDANCE_ZONES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn404WhenDeletingAvoidanceZoneOfAnotherUser() throws Exception {
        // Given
        Long zoneId = createDefaultAvoidanceZone(userAccessToken);
        String thiefAccessToken = registerUserAndGetAccessToken("avoidance.thief@example.com", "ThiefPassword123!", "Avoidance Thief");

        // When & Then
        mockMvc.perform(delete(AVOIDANCE_ZONES_URL + "/" + zoneId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + thiefAccessToken))
                .andExpect(status().isNotFound());
    }

    private String registerUserAndGetAccessToken(String email, String password, String fullName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, fullName);
        String responseContent = mockMvc.perform(post(AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, AuthResponse.class).accessToken();
    }

    private Long createDefaultAvoidanceZone(String token) throws Exception {
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(TEST_ZONE_NAME, VALID_WKT_POLYGON);
        String responseContent = mockMvc.perform(post(AVOIDANCE_ZONES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, AvoidanceZoneDto.class).id();
    }
}
