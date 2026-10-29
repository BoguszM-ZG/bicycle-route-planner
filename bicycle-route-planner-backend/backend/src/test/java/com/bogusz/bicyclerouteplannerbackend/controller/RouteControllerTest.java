package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.CreateRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.ElevationPointDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.FileExportDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RenameRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteSummaryDto;
import com.bogusz.bicyclerouteplannerbackend.model.RouteType;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import com.bogusz.bicyclerouteplannerbackend.service.RouteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RouteController.class)
@AutoConfigureMockMvc(addFilters = false)
class RouteControllerTest {

    private static final long USER_ID = 99L;
    private static final long ROUTE_ID = 1L;
    private static final String ROUTE_NAME = "Test Route";
    private static final String ROUTES_URL = "/api/routes";
    private static final String ROUTE_GEOMETRY = "LINESTRING Z(1 1 100, 2 2 150)";
    private static final String START_POINT = "POINT(1 1)";
    private static final String USER_EMAIL = "test@test.com";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Authentication auth = getMockAuthentication();
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturn200AndListOfRoutesWhenGettingAllRoutes() throws Exception {
        // GIVEN
        RouteSummaryDto summaryDto = new RouteSummaryDto(ROUTE_ID, ROUTE_NAME, 5000.0, null);

        given(routeService.getRoutesForUser(USER_ID)).willReturn(List.of(summaryDto));

        // WHEN
        ResultActions resultActions = mockMvc.perform(get(ROUTES_URL));

        // THEN
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(ROUTE_ID))
                .andExpect(jsonPath("$[0].name").value(ROUTE_NAME))
                .andExpect(jsonPath("$[0].distanceMeters").value(5000.0));
    }

    @Test
    void shouldIgnoreFolderIdQueryParamAndReturnAllRoutes() throws Exception {
        // GIVEN
        RouteSummaryDto summaryDto = new RouteSummaryDto(ROUTE_ID, ROUTE_NAME, 5000.0, null);

        given(routeService.getRoutesForUser(USER_ID)).willReturn(List.of(summaryDto));

        // WHEN
        ResultActions resultActions = mockMvc.perform(get(ROUTES_URL).param("folderId", "5"));

        // THEN
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(ROUTE_ID));
        then(routeService).should().getRoutesForUser(USER_ID);
    }

    @Test
    void shouldReturn200AndRouteDetailWhenRouteExists() throws Exception {
        // GIVEN
        RouteDetailDto detailDto = RouteDetailDto.builder()
                .id(ROUTE_ID)
                .name(ROUTE_NAME)
                .routeType(RouteType.POINT_TO_POINT)
                .targetDistance(5000.0)
                .distanceMeters(5000.0)
                .elevationGain(150.0)
                .elevationLoss(0.0)
                .startPoint(START_POINT)
                .routeGeom(ROUTE_GEOMETRY)
                .createdAt(Instant.now())
                .folderId(null)
                .build();

        given(routeService.getRouteDetail(ROUTE_ID, USER_ID)).willReturn(detailDto);

        // WHEN
        ResultActions resultActions = mockMvc.perform(get(ROUTES_URL + "/" + ROUTE_ID));

        // THEN
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ROUTE_ID))
                .andExpect(jsonPath("$.name").value(ROUTE_NAME))
                .andExpect(jsonPath("$.routeGeom").value(ROUTE_GEOMETRY));
    }

    @Test
    void shouldReturn200AndGpxFileWhenExportingRoute() throws Exception {
        // GIVEN
        byte[] gpxData = "<gpx>Test GPX</gpx>".getBytes();
        String expectedFileName = "Test Route.gpx";
        FileExportDto exportDto = new FileExportDto(gpxData, expectedFileName);
        given(routeService.exportRouteToGpx(ROUTE_ID, USER_ID)).willReturn(exportDto);

        // WHEN
        ResultActions resultActions = mockMvc.perform(get(ROUTES_URL + "/" + ROUTE_ID + "/export/gpx"));

        // THEN
        resultActions.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/gpx+xml"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Test Route.gpx\"; filename*=UTF-8''Test%20Route.gpx"))
                .andExpect(content().bytes(gpxData));
    }

    @Test
    void shouldReturn200AndGeoJsonFileWhenExportingRoute() throws Exception {
        // GIVEN
        byte[] geoJsonData = "{\"type\":\"Feature\"}".getBytes();
        given(routeService.exportRouteToGeoJson(ROUTE_ID, USER_ID))
                .willReturn(new FileExportDto(geoJsonData, "Test Route.geojson"));

        // WHEN
        ResultActions resultActions = mockMvc.perform(get(ROUTES_URL + "/" + ROUTE_ID + "/export/geojson"));

        // THEN
        resultActions.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/geo+json"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Test Route.geojson\"; filename*=UTF-8''Test%20Route.geojson"))
                .andExpect(content().bytes(geoJsonData));
    }

    @Test
    void shouldReturn200AndElevationPointsWhenGettingElevation() throws Exception {
        // GIVEN
        ElevationPointDto point = new ElevationPointDto(0.0, 150.0);
        given(routeService.getElevationPointsForRoute(ROUTE_ID, USER_ID)).willReturn(List.of(point));

        // WHEN
        ResultActions resultActions = mockMvc.perform(get(ROUTES_URL + "/" + ROUTE_ID + "/elevation"));

        // THEN
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].distance").value(0.0))
                .andExpect(jsonPath("$[0].elevation").value(150.0));
    }

    @Test
    void shouldReturn201WhenSavingValidRoute() throws Exception {
        // GIVEN
        CreateRouteRequestDto requestDto = createValidRequestDto();
        RouteDetailDto responseDto = createResponseDto();

        given(routeService.saveRouteForUser(any(), eq(USER_ID))).willReturn(responseDto);

        // WHEN
        ResultActions resultActions = mockMvc.perform(post(ROUTES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)));

        // THEN
        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ROUTE_ID))
                .andExpect(jsonPath("$.name").value(ROUTE_NAME));

        ArgumentCaptor<CreateRouteRequestDto> createRequestCaptor = ArgumentCaptor.forClass(CreateRouteRequestDto.class);
        then(routeService).should().saveRouteForUser(createRequestCaptor.capture(), eq(USER_ID));
        
        CreateRouteRequestDto captured = createRequestCaptor.getValue();
        assertThat(captured.name()).isEqualTo(ROUTE_NAME);
        assertThat(captured.routeType()).isEqualTo(RouteType.POINT_TO_POINT);
        assertThat(captured.targetDistance()).isEqualTo(5000.0);
    }

    @Test
    void shouldReturn400BadRequestWhenRouteNameIsInvalid() throws Exception {
        // GIVEN
        CreateRouteRequestDto badRequest = createInvalidRequestDto();

        // WHEN
        ResultActions resultActions = mockMvc.perform(post(ROUTES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)));

        // THEN
        resultActions.andExpect(status().isBadRequest());
        then(routeService).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturn400BadRequestWhenRouteGeometryHasCoordinatesOutOfBounds() throws Exception {
        // GIVEN
        CreateRouteRequestDto badRequest = new CreateRouteRequestDto(
                ROUTE_NAME,
                RouteType.POINT_TO_POINT,
                5000.0,
                5000.0,
                100.0,
                50.0,
                START_POINT,
                "LINESTRING(19.945 100.0, 19.946 50.0648)",
                null
        );

        // WHEN
        ResultActions resultActions = mockMvc.perform(post(ROUTES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)));

        // THEN
        resultActions.andExpect(status().isBadRequest());
        then(routeService).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturn204WhenUpdatingRouteFolder() throws Exception {
        // GIVEN
        UpdateRouteFolderDto request = new UpdateRouteFolderDto(2L);

        // WHEN
        ResultActions resultActions = mockMvc.perform(patch(ROUTES_URL + "/" + ROUTE_ID + "/folder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        // THEN
        resultActions.andExpect(status().isNoContent());

        ArgumentCaptor<UpdateRouteFolderDto> updateFolderCaptor = ArgumentCaptor.forClass(UpdateRouteFolderDto.class);
        then(routeService).should().updateRouteFolder(eq(ROUTE_ID), updateFolderCaptor.capture(), eq(USER_ID));
        
        UpdateRouteFolderDto captured = updateFolderCaptor.getValue();
        assertThat(captured.folderId()).isEqualTo(2L);
    }

    @Test
    void shouldReturn204WhenRenamingRoute() throws Exception {
        // GIVEN
        RenameRouteRequestDto request = new RenameRouteRequestDto("New Route Name");

        // WHEN
        ResultActions resultActions = mockMvc.perform(patch(ROUTES_URL + "/" + ROUTE_ID + "/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        // THEN
        resultActions.andExpect(status().isNoContent());

        ArgumentCaptor<RenameRouteRequestDto> renameRequestCaptor = ArgumentCaptor.forClass(RenameRouteRequestDto.class);
        then(routeService).should().renameRoute(eq(ROUTE_ID), renameRequestCaptor.capture(), eq(USER_ID));
        
        RenameRouteRequestDto captured = renameRequestCaptor.getValue();
        assertThat(captured.name()).isEqualTo("New Route Name");
    }

    @Test
    void shouldReturn400BadRequestWhenRenamingRouteWithBlankName() throws Exception {
        // GIVEN
        RenameRouteRequestDto badRequest = new RenameRouteRequestDto(""); 

        // WHEN
        ResultActions resultActions = mockMvc.perform(patch(ROUTES_URL + "/" + ROUTE_ID + "/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)));

        // THEN
        resultActions.andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.name").value("Name is required"));

        then(routeService).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturn400BadRequestWhenRenamingRouteWithNameExceedingLimit() throws Exception {
        // GIVEN
        RenameRouteRequestDto badRequest = new RenameRouteRequestDto("a".repeat(151));

        // WHEN
        ResultActions resultActions = mockMvc.perform(patch(ROUTES_URL + "/" + ROUTE_ID + "/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)));

        // THEN
        resultActions.andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.name").value("Name cannot exceed 150 characters"));

        then(routeService).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturn204WhenDeletingRoute() throws Exception {
        // GIVEN
        long routeIdToDelete = ROUTE_ID;

        // WHEN
        ResultActions resultActions = mockMvc.perform(delete(ROUTES_URL + "/" + routeIdToDelete));

        // THEN
        resultActions.andExpect(status().isNoContent());
        then(routeService).should().deleteRouteForUser(routeIdToDelete, USER_ID);
    }

    private Authentication getMockAuthentication() {
        UserPrincipal principal = UserPrincipal.of(USER_ID, USER_EMAIL);
        return new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
    }

    private CreateRouteRequestDto createValidRequestDto() {
        return new CreateRouteRequestDto(
                ROUTE_NAME,
                RouteType.POINT_TO_POINT,
                5000.0,
                5000.0,
                150.0,
                0.0,
                START_POINT,
                ROUTE_GEOMETRY,
                null
        );
    }

    private CreateRouteRequestDto createInvalidRequestDto() {
        return new CreateRouteRequestDto(
                "", 
                RouteType.POINT_TO_POINT,
                5000.0,
                5000.0,
                150.0,
                0.0,
                START_POINT,
                ROUTE_GEOMETRY,
                null
        );
    }

    private RouteDetailDto createResponseDto() {
        return RouteDetailDto.builder()
                .id(ROUTE_ID)
                .name(ROUTE_NAME)
                .routeType(RouteType.POINT_TO_POINT)
                .targetDistance(5000.0)
                .distanceMeters(5000.0)
                .elevationGain(150.0)
                .elevationLoss(0.0)
                .startPoint(START_POINT)
                .routeGeom(ROUTE_GEOMETRY)
                .createdAt(Instant.now())
                .folderId(null)
                .build();
    }
}
