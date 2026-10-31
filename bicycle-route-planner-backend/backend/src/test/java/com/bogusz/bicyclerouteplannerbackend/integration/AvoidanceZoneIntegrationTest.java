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
import org.springframework.test.web.servlet.ResultActions;
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
    private static final String LARGE_WKT_POLYGON_A = "POLYGON ((19.0 50.0, 19.25 50.0, 19.25 50.06, 19.0 50.06, 19.0 50.0))";
    private static final String LARGE_WKT_POLYGON_B = "POLYGON ((19.0 50.1, 19.25 50.1, 19.25 50.16, 19.0 50.16, 19.0 50.1))";
    private static final String TOO_WIDE_WKT_POLYGON = "POLYGON ((19.0 50.0, 19.35 50.0, 19.35 50.01, 19.0 50.01, 19.0 50.0))";
    private static final String LIMIT_EXCEEDED_TYPE = "urn:problem-type:avoidance-zone-limit-exceeded";

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
    void shouldRejectZoneWiderThan20Km() throws Exception {
        // When & Then
        createZone(TOO_WIDE_WKT_POLYGON)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(LIMIT_EXCEEDED_TYPE));

        assertZoneCount(0);
    }

    @Test
    void shouldRejectZoneWhenTotalActiveAreaExceeds200SquareKm() throws Exception {
        // Given
        createZone(LARGE_WKT_POLYGON_A).andExpect(status().isCreated());

        // When & Then
        createZone(LARGE_WKT_POLYGON_B)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(LIMIT_EXCEEDED_TYPE));

        assertZoneCount(1);
    }

    @Test
    void shouldAllowActivatingZoneOnlyWhenTotalActiveAreaStaysWithinLimit() throws Exception {
        // Given
        Long zoneAId = readZoneId(createZone(LARGE_WKT_POLYGON_A).andExpect(status().isCreated()));
        setZoneActive(zoneAId, false).andExpect(status().isNoContent());
        Long zoneBId = readZoneId(createZone(LARGE_WKT_POLYGON_B).andExpect(status().isCreated()));

        // When & Then
        setZoneActive(zoneAId, true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(LIMIT_EXCEEDED_TYPE));

        setZoneActive(zoneBId, false).andExpect(status().isNoContent());
        setZoneActive(zoneAId, true).andExpect(status().isNoContent());
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

    private ResultActions createZone(String wkt) throws Exception {
        CreateAvoidanceZoneRequestDto request = new CreateAvoidanceZoneRequestDto(TEST_ZONE_NAME, wkt);
        return mockMvc.perform(post(AVOIDANCE_ZONES_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));
    }

    private ResultActions setZoneActive(Long zoneId, boolean isActive) throws Exception {
        return mockMvc.perform(patch(AVOIDANCE_ZONES_URL + "/" + zoneId + "/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new UpdateAvoidanceZoneStatusDto(isActive))));
    }

    private Long readZoneId(ResultActions resultActions) throws Exception {
        String responseContent = resultActions.andReturn().getResponse().getContentAsString();
        return jsonMapper.readValue(responseContent, AvoidanceZoneDto.class).id();
    }

    private void assertZoneCount(int expectedCount) throws Exception {
        mockMvc.perform(get(AVOIDANCE_ZONES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(expectedCount));
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
