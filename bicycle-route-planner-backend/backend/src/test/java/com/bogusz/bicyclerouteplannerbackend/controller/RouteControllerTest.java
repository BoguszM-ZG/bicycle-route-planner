package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.CreateRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.ElevationPointDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.GpxExportDto;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private static final long ROUTE_ID = 1L;
    private static final long USER_ID = 99L;
    private static final String ROUTE_NAME = "Test Route";
    private static final String ROUTE_GEOMETRY = "LINESTRING Z(1 1 100, 2 2 150)";
    private static final String START_POINT = "POINT(1 1)";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(getMockAuthentication());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturn200AndRouteSummaryListWhenGettingAllRoutes() throws Exception {
        // Given
        RouteSummaryDto summaryDto = new RouteSummaryDto(ROUTE_ID, ROUTE_NAME, 5000.0, null);
        given(routeService.getRoutesForUser(USER_ID, null)).willReturn(List.of(summaryDto));

        // When & Then
        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(ROUTE_ID))
                .andExpect(jsonPath("$[0].name").value(ROUTE_NAME));
    }

    @Test
    void shouldReturn200AndRouteDetailWhenGettingRouteById() throws Exception {
        // Given
        RouteDetailDto detailDto = createResponseDto();
        given(routeService.getRouteDetail(ROUTE_ID, USER_ID)).willReturn(detailDto);

        // When & Then
        mockMvc.perform(get("/api/routes/" + ROUTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ROUTE_ID))
                .andExpect(jsonPath("$.name").value(ROUTE_NAME))
                .andExpect(jsonPath("$.routeGeom").value(ROUTE_GEOMETRY));
    }

    @Test
    void shouldReturn200AndGpxFileWhenExportingRoute() throws Exception {
        // Given
        byte[] gpxData = "<gpx>Test GPX</gpx>".getBytes();
        GpxExportDto exportDto = new GpxExportDto(gpxData, ROUTE_NAME);
        given(routeService.exportRouteToGpx(ROUTE_ID, USER_ID)).willReturn(exportDto);

        // When & Then
        mockMvc.perform(get("/api/routes/" + ROUTE_ID + "/export/gpx"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"attachment\"; filename=\"Test_Route.gpx\""))
                .andExpect(content().bytes(gpxData));
    }

    @Test
    void shouldReturn200AndElevationPointsWhenGettingElevation() throws Exception {
        // Given
        ElevationPointDto point = new ElevationPointDto(0.0, 150.0);
        given(routeService.getElevationPointsForRoute(ROUTE_ID, USER_ID)).willReturn(List.of(point));

        // When & Then
        mockMvc.perform(get("/api/routes/" + ROUTE_ID + "/elevation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].distance").value(0.0))
                .andExpect(jsonPath("$[0].elevation").value(150.0));
    }

    @Test
    void shouldReturn201WhenSavingValidRoute() throws Exception {
        // Given
        CreateRouteRequestDto requestDto = createValidRequestDto();
        RouteDetailDto responseDto = createResponseDto();

        given(routeService.saveRouteForUser(any(), eq(USER_ID))).willReturn(responseDto);

        // When
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ROUTE_ID))
                .andExpect(jsonPath("$.name").value(ROUTE_NAME));

        // Then
        ArgumentCaptor<CreateRouteRequestDto> createRequestCaptor = ArgumentCaptor.forClass(CreateRouteRequestDto.class);
        then(routeService).should(times(1)).saveRouteForUser(createRequestCaptor.capture(), eq(USER_ID));
        
        CreateRouteRequestDto captured = createRequestCaptor.getValue();
        assertThat(captured.name()).isEqualTo(ROUTE_NAME);
        assertThat(captured.routeType()).isEqualTo(RouteType.POINT_TO_POINT);
        assertThat(captured.targetDistance()).isEqualTo(5000.0);
    }

    @Test
    void shouldReturn400BadRequestWhenRouteNameIsInvalid() throws Exception {
        // Given
        CreateRouteRequestDto badRequest = createInvalidRequestDto();

        // When & Then
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(routeService).should(never()).saveRouteForUser(any(), any());
    }

    @Test
    void shouldReturn204WhenUpdatingRouteFolder() throws Exception {
        // Given
        UpdateRouteFolderDto request = new UpdateRouteFolderDto(2L);

        // When
        mockMvc.perform(patch("/api/routes/" + ROUTE_ID + "/folder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then
        ArgumentCaptor<UpdateRouteFolderDto> updateFolderCaptor = ArgumentCaptor.forClass(UpdateRouteFolderDto.class);
        then(routeService).should(times(1)).updateRouteFolder(eq(ROUTE_ID), updateFolderCaptor.capture(), eq(USER_ID));
        
        UpdateRouteFolderDto captured = updateFolderCaptor.getValue();
        assertThat(captured.folderId()).isEqualTo(2L);
    }

    @Test
    void shouldReturn204WhenRenamingRoute() throws Exception {
        // Given
        String newName = "New Route Name";
        RenameRouteRequestDto request = new RenameRouteRequestDto(newName);

        // When
        mockMvc.perform(patch("/api/routes/" + ROUTE_ID + "/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then
        ArgumentCaptor<RenameRouteRequestDto> renameRequestCaptor = ArgumentCaptor.forClass(RenameRouteRequestDto.class);
        then(routeService).should(times(1)).renameRoute(eq(ROUTE_ID), renameRequestCaptor.capture(), eq(USER_ID));
        
        RenameRouteRequestDto captured = renameRequestCaptor.getValue();
        assertThat(captured.name()).isEqualTo(newName);
    }

    @Test
    void shouldReturn400BadRequestWhenRenamingRouteWithBlankName() throws Exception {
        // Given
        RenameRouteRequestDto badRequest = new RenameRouteRequestDto(""); 

        // When & Then
        mockMvc.perform(patch("/api/routes/" + ROUTE_ID + "/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(routeService).should(never()).renameRoute(any(), any(), any());
    }

    @Test
    void shouldReturn204WhenDeletingRoute() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/routes/" + ROUTE_ID))
                .andExpect(status().isNoContent());

        // Then
        then(routeService).should(times(1)).deleteRouteForUser(ROUTE_ID, USER_ID);
    }

    private Authentication getMockAuthentication() {
        UserPrincipal principal = new UserPrincipal(USER_ID, "test@test.com");
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