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
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private static final String TEST_EMAIL = "routeuser@test.com";
    private static final String TEST_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @Autowired
    private RouteRepository routeRepository;

    private String userAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        userAccessToken = registerUserAndGetAccessToken(TEST_EMAIL, TEST_PASSWORD, "Route User");
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
                TEST_ELEVATION_GAIN, TEST_ELEVATION_LOSS, "NOT_A_GEOMETRY", VALID_WKT_LINESTRING, null
        );

        // When
        mockMvc.perform(post(ROUTES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"));
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
        String otherUserToken = registerUserAndGetAccessToken("other@test.com", "Password123!", "Other User");

        // When
        mockMvc.perform(delete(ROUTES_URL + "/" + routeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Route Not Found"));
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
    void shouldDownsampleLongRouteElevationPoints() throws Exception {
        // Given
        StringBuilder linestring = new StringBuilder("LINESTRING Z(");
        for (int i = 0; i < 300; i++) {
            if (i > 0) {
                linestring.append(", ");
            }
            linestring.append(15.0 + (i * 0.001)).append(" ")
                    .append(50.0 + (i * 0.001)).append(" ")
                    .append(100.0 + (i % 50));
        }
        linestring.append(")");

        CreateRouteRequestDto request = new CreateRouteRequestDto(
                TEST_ROUTE_NAME,
                TEST_ROUTE_TYPE,
                TEST_TARGET_DISTANCE,
                TEST_DISTANCE_METERS,
                TEST_ELEVATION_GAIN,
                TEST_ELEVATION_LOSS,
                VALID_WKT_POINT,
                linestring.toString(),
                null
        );

        String responseStr = mockMvc.perform(post(ROUTES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long routeId = jsonMapper.readValue(responseStr, RouteDetailDto.class).id();

        // When & Then
        mockMvc.perform(get(ROUTES_URL + "/" + routeId + "/elevation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(250)));

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
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/gpx+xml")))
                .andReturn().getResponse().getContentAsByteArray();

        // Then
        assertThat(content).isNotEmpty();
    }

    @Test
    void shouldExportRouteToGeoJsonGeneratedByPostgis() throws Exception {
        // Given
        long routeId = createRouteAndGetId();

        // When
        mockMvc.perform(get(ROUTES_URL + "/" + routeId + "/export/geojson")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/geo+json")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"My Test Route.geojson\"; filename*=UTF-8''My%20Test%20Route.geojson"))
                .andExpect(jsonPath("$.type").value("Feature"))
                .andExpect(jsonPath("$.geometry.type").value("LineString"))
                .andExpect(jsonPath("$.geometry.coordinates.length()").value(2))
                .andExpect(jsonPath("$.geometry.coordinates[0][2]").value(100.0))
                .andExpect(jsonPath("$.properties.name").value(TEST_ROUTE_NAME))
                .andExpect(jsonPath("$.properties.distance_meters").value(TEST_DISTANCE_METERS));
    }

    @Test
    void shouldReturn404WhenExportingGeoJsonOfAnotherUserRoute() throws Exception {
        // Given
        long routeId = createRouteAndGetId();
        String otherUserToken = registerUserAndGetAccessToken("other@test.com", "Password123!", "Other User");

        // When
        mockMvc.perform(get(ROUTES_URL + "/" + routeId + "/export/geojson")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Route Not Found"));
    }

    @Test
    void shouldRenameRoute() throws Exception {
        // Given
        long routeId = createRouteAndGetId();
        RenameRouteRequestDto renameRequest = new RenameRouteRequestDto("New Awesome Route");

        // When
        mockMvc.perform(patch(ROUTES_URL + "/" + routeId + "/name")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(renameRequest)))
                // Then
                .andExpect(status().isNoContent());

        // Then
        Route route = routeRepository.findById(routeId).orElseThrow();
        assertThat(route.getName()).isEqualTo("New Awesome Route");
    }

    @Test
    void shouldReturn400WhenRenamingRouteWithBlankName() throws Exception {
        // Given
        long routeId = createRouteAndGetId();
        RenameRouteRequestDto renameRequest = new RenameRouteRequestDto("   ");

        // When & Then
        mockMvc.perform(patch(ROUTES_URL + "/" + routeId + "/name")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(renameRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").value("Name is required"));
    }

    @Test
    void shouldReturn400WhenRenamingRouteWithNameExceedingLimit() throws Exception {
        // Given
        long routeId = createRouteAndGetId();
        RenameRouteRequestDto renameRequest = new RenameRouteRequestDto("a".repeat(151));

        // When & Then
        mockMvc.perform(patch(ROUTES_URL + "/" + routeId + "/name")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(renameRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").value("Name cannot exceed 150 characters"));
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

    @Test
    void shouldReturn401ProblemDetailWhenRequestIsAnonymous() throws Exception {
        mockMvc.perform(get(ROUTES_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn401ProblemDetailWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get(ROUTES_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
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

