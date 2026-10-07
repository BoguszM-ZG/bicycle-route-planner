package com.bogusz.bicyclerouteplannerbackend.integration;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.CreateRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RenameRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.model.RouteType;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class RouteIntegrationTest extends BaseIntegrationTest {

    private static final String ROUTES_URL = "/api/routes";
    private static final String REGISTER_URL = "/api/auth/register";
    private static final String TEST_ROUTE_NAME = "My Test Route";
    private static final RouteType TEST_ROUTE_TYPE = RouteType.POINT_TO_POINT;
    private static final double TEST_TARGET_DISTANCE = 2000.0;
    private static final double TEST_DISTANCE_METERS = 1500.5;
    private static final double TEST_ELEVATION_GAIN = 50.0;
    private static final double TEST_ELEVATION_LOSS = 20.0;
    
    private static final String VALID_WKT_POINT = "POINT(15.0 50.0)";
    private static final String VALID_WKT_LINESTRING = "LINESTRING Z(15.0 50.0 100.0, 15.1 50.1 110.0)";
    private static final String INVALID_GEOMETRY = "NOT_A_GEOMETRY";
    
    private static final String TEST_EMAIL = "routeuser@test.com";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_FULL_NAME = "Route User";

    private static final String OTHER_USER_EMAIL = "other@test.com";
    private static final String OTHER_USER_PASSWORD = "Password123!";
    private static final String OTHER_USER_FULL_NAME = "Other User";
    
    private static final String NEW_TEST_ROUTE_NAME = "New Awesome Route";
    private static final String ERROR_INVALID_GEOMETRY = "Invalid Geometry Parsing Error";
    private static final String ERROR_ROUTE_NOT_FOUND = "Route Not Found";
    
    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @Autowired
    private RouteRepository routeRepository;


    private String userAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        userAccessToken = registerUserAndGetAccessToken(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);
    }

    @Test
    void shouldSaveRouteAndReturn201() throws Exception {
        // Given
        CreateRouteRequestDto request = createValidRouteRequest();

        // When
        mockMvc.perform(post(ROUTES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(TEST_ROUTE_NAME))
                .andExpect(jsonPath("$.distanceMeters").value(TEST_DISTANCE_METERS));

        // Then
        assertThat(routeRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldReturn400WhenSavingRouteWithInvalidGeometry() throws Exception {
        // Given
        CreateRouteRequestDto request = new CreateRouteRequestDto(
                TEST_ROUTE_NAME, TEST_ROUTE_TYPE, TEST_TARGET_DISTANCE, TEST_DISTANCE_METERS, 
                TEST_ELEVATION_GAIN, TEST_ELEVATION_LOSS, INVALID_GEOMETRY, VALID_WKT_LINESTRING, null
        );

        // When
        mockMvc.perform(post(ROUTES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(ERROR_INVALID_GEOMETRY));
    }

    @Test
    void shouldGetAllRoutesForUser() throws Exception {
        // Given

        createRouteAndGetId();
        createRouteAndGetId();

        // When
        mockMvc.perform(get(ROUTES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldDeleteRoute() throws Exception {
        // Given
        long routeId = createRouteAndGetId();

        // When
        mockMvc.perform(delete(ROUTES_URL + "/" + routeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                // Then
                .andExpect(status().isNoContent());

        // Then
        assertThat(routeRepository.findById(routeId)).isEmpty();
    }

    @Test
    void shouldReturn404WhenDeletingRouteOfAnotherUser() throws Exception {
        // Given
        long routeId = createRouteAndGetId();
        String otherUserToken = registerUserAndGetAccessToken(OTHER_USER_EMAIL, OTHER_USER_PASSWORD, OTHER_USER_FULL_NAME);

        // When
        mockMvc.perform(delete(ROUTES_URL + "/" + routeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(ERROR_ROUTE_NOT_FOUND));
    }


    @Test
    void shouldGetRouteDetail() throws Exception {
        // Given
        long routeId = createRouteAndGetId();

        // When
        mockMvc.perform(get(ROUTES_URL + "/" + routeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(routeId))
                .andExpect(jsonPath("$.name").value(TEST_ROUTE_NAME));
    }

    @Test
    void shouldGetElevationPoints() throws Exception {
        // Given
        long routeId = createRouteAndGetId();

        // When
        mockMvc.perform(get(ROUTES_URL + "/" + routeId + "/elevation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    void shouldExportRouteToGpx() throws Exception {
        // Given
        long routeId = createRouteAndGetId();

        // When
        byte[] content = mockMvc.perform(get(ROUTES_URL + "/" + routeId + "/export/gpx")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                // Then
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn().getResponse().getContentAsByteArray();

        // Then
        assertThat(content).isNotEmpty();
    }

    @Test
    void shouldRenameRoute() throws Exception {
        // Given
        long routeId = createRouteAndGetId();
        RenameRouteRequestDto renameRequest = new RenameRouteRequestDto(NEW_TEST_ROUTE_NAME);

        // When
        mockMvc.perform(patch(ROUTES_URL + "/" + routeId + "/name")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(renameRequest)))
                // Then
                .andExpect(status().isNoContent());

        // Then
        Route route = routeRepository.findById(routeId).orElseThrow();
        assertThat(route.getName()).isEqualTo(NEW_TEST_ROUTE_NAME);
    }

    @Test
    void shouldUpdateRouteFolderToNull() throws Exception {
        // Given
        long routeId = createRouteAndGetId();
        UpdateRouteFolderDto folderRequest = new UpdateRouteFolderDto(null);

        // When
        mockMvc.perform(patch(ROUTES_URL + "/" + routeId + "/folder")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(folderRequest)))
                // Then
                .andExpect(status().isNoContent());

        // Then
        Route route = routeRepository.findById(routeId).orElseThrow();
        assertThat(route.getFolder()).isNull();
    }


    private long createRouteAndGetId() throws Exception {
        CreateRouteRequestDto request = createValidRouteRequest();
        String responseStr = mockMvc.perform(post(ROUTES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseStr, RouteDetailDto.class).id();
    }

    private CreateRouteRequestDto createValidRouteRequest() {
        return new CreateRouteRequestDto(
                TEST_ROUTE_NAME,
                TEST_ROUTE_TYPE,
                TEST_TARGET_DISTANCE,
                TEST_DISTANCE_METERS,
                TEST_ELEVATION_GAIN,
                TEST_ELEVATION_LOSS,
                VALID_WKT_POINT,
                VALID_WKT_LINESTRING,
                null
        );
    }


    private String registerUserAndGetAccessToken(String email, String password, String fullName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, fullName);
        String responseContent = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, AuthResponse.class).accessToken();
    }
}



