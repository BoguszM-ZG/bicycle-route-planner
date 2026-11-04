package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.route.CreateRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteDetailDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RouteSummaryDto;
import com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.exception.RouteNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.mapper.RouteMapper;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import com.bogusz.bicyclerouteplannerbackend.model.RouteType;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.AvoidanceZoneRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.FolderRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.RouteRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.UpdateRouteFolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.ElevationPointDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.FileExportDto;
import com.bogusz.bicyclerouteplannerbackend.dto.route.RenameRouteRequestDto;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long FOLDER_ID = 10L;
    private static final Long ROUTE_ID = 100L;
    private static final String ROUTE_NAME = "Route";
    private static final RouteType ROUTE_TYPE = RouteType.POINT_TO_POINT;
    private static final Double TARGET_DISTANCE = 10000.0;
    private static final Double DISTANCE_METERS = 10000.0;
    private static final Double ELEVATION_GAIN = 100.0;
    private static final Double ELEVATION_LOSS = 50.0;
    private static final String START_POINT = "POINT(1 1)";
    private static final String ROUTE_GEOM = "LINESTRING(1 1, 2 2)";

    @Mock private RouteRepository routeRepository;
    @Mock private UserRepository userRepository;
    @Mock private GpxExportService gpxExportService;
    @Mock private FolderRepository folderRepository;
    @Mock private AvoidanceZoneRepository avoidanceZoneRepository;
    @Mock private RouteMapper routeMapper;
    @Spy private ElevationProfileDownsampler elevationProfileDownsampler = new ElevationProfileDownsampler();

    @InjectMocks
    private RouteService routeService;

    @Test
    void shouldSaveRouteForUserWithoutFolder() {
        // GIVEN
        CreateRouteRequestDto request = new CreateRouteRequestDto(
                ROUTE_NAME, ROUTE_TYPE, TARGET_DISTANCE, DISTANCE_METERS,
                ELEVATION_GAIN, ELEVATION_LOSS, START_POINT, ROUTE_GEOM, null
        );
        User mockUser = new User();
        Route mockRoute = new Route();
        RouteDetailDto mockResponseDto = RouteDetailDto.builder().id(ROUTE_ID).name(ROUTE_NAME).build();

        given(userRepository.getReferenceById(USER_ID)).willReturn(mockUser);
        given(routeMapper.toEntity(request, mockUser, null)).willReturn(mockRoute);
        given(routeRepository.save(mockRoute)).willReturn(mockRoute);
        given(routeMapper.toRouteDetailDto(mockRoute)).willReturn(mockResponseDto);

        // WHEN
        RouteDetailDto result = routeService.saveRouteForUser(request, USER_ID);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(ROUTE_ID);

        then(folderRepository).shouldHaveNoInteractions();
        then(routeRepository).should().save(mockRoute);
    }

    @Test
    void shouldThrowExceptionWhenSavingRouteToNonExistentFolder() {
        // GIVEN
        CreateRouteRequestDto request = new CreateRouteRequestDto(
                ROUTE_NAME, ROUTE_TYPE, TARGET_DISTANCE, DISTANCE_METERS,
                ELEVATION_GAIN, ELEVATION_LOSS, START_POINT, ROUTE_GEOM, FOLDER_ID
        );

        given(folderRepository.findByIdAndUserId(FOLDER_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.saveRouteForUser(request, USER_ID))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Folder not found");

        then(routeRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldGetRoutesForUser() {
        // GIVEN
        RouteSummaryDto summaryDto = mock(RouteSummaryDto.class);
        given(routeRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(List.of(summaryDto));

        // WHEN
        List<RouteSummaryDto> result = routeService.getRoutesForUser(USER_ID);

        // THEN
        assertThat(result).hasSize(1);
        then(routeRepository).should().findAllByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoRoutes() {
        // GIVEN
        given(routeRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(List.of());

        // WHEN
        List<RouteSummaryDto> result = routeService.getRoutesForUser(USER_ID);

        // THEN
        assertThat(result).isEmpty();
        then(routeRepository).should().findAllByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    void shouldDeleteRouteForUserSuccessfully() {
        // GIVEN
        Route mockRoute = new Route();
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));

        // WHEN
        routeService.deleteRouteForUser(ROUTE_ID, USER_ID);

        // THEN
        then(routeRepository).should().delete(mockRoute);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentRoute() {
        // GIVEN
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.deleteRouteForUser(ROUTE_ID, USER_ID))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");

        then(routeRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldExportRouteToGpxSuccessfully() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName(ROUTE_NAME);
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));

        byte[] fakeGpxData = "<gpx>Route</gpx>".getBytes();
        given(gpxExportService.generateGpxFile(mockRoute)).willReturn(fakeGpxData);
        // WHEN
        FileExportDto result = routeService.exportRouteToGpx(ROUTE_ID, USER_ID);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.fileName()).isEqualTo("Route.gpx");
        assertThat(result.data()).isEqualTo(fakeGpxData);
    }

    @Test
    void shouldExportRouteToGeoJsonSuccessfully() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName(ROUTE_NAME);
        String geoJson = "{\"type\":\"Feature\"}";
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        given(routeRepository.findGeoJsonById(ROUTE_ID)).willReturn(geoJson);

        // WHEN
        FileExportDto result = routeService.exportRouteToGeoJson(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result.fileName()).isEqualTo("Route.geojson");
        assertThat(result.data()).isEqualTo(geoJson.getBytes());
    }

    @Test
    void shouldThrowRouteNotFoundExceptionWhenExportingNonExistentRouteToGeoJson() {
        // GIVEN
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.exportRouteToGeoJson(ROUTE_ID, USER_ID))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");

        then(routeRepository).should().findByIdAndUserId(ROUTE_ID, USER_ID);
        then(routeRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldRenameRouteSuccessfully() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName("Old name");

        RenameRouteRequestDto request = new RenameRouteRequestDto("New name");
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        // WHEN
        routeService.renameRoute(ROUTE_ID, request, USER_ID);
        // THEN
        assertThat(mockRoute.getName()).isEqualTo("New name");
    }
    @Test
    void shouldUpdateRouteFolderSuccessfully() {
        // GIVEN
        Route mockRoute = new Route();
        Folder mockFolder = new Folder();
        UpdateRouteFolderDto request = new UpdateRouteFolderDto(FOLDER_ID);
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        given(folderRepository.findByIdAndUserId(FOLDER_ID, USER_ID)).willReturn(Optional.of(mockFolder));
        // WHEN
        routeService.updateRouteFolder(ROUTE_ID, request, USER_ID);
        // THEN
        assertThat(mockRoute.getFolder()).isEqualTo(mockFolder);
    }
    @Test
    void shouldRemoveRouteFromFolderWhenFolderIdIsNull() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setFolder(new Folder());
        UpdateRouteFolderDto request = new UpdateRouteFolderDto(null);
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        // WHEN
        routeService.updateRouteFolder(ROUTE_ID, request, USER_ID);
        // THEN
        assertThat(mockRoute.getFolder()).isNull();
        then(folderRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnElevationProfileCalculatedByRepository() {
        // GIVEN
        List<ElevationPointDto> profile = List.of(
                new ElevationPointDto(0.0, 200.0),
                new ElevationPointDto(1112.0, 210.0)
        );
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(new Route()));
        given(routeRepository.findElevationProfileById(ROUTE_ID)).willReturn(profile);

        // WHEN
        List<ElevationPointDto> result = routeService.getElevationPointsForRoute(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result).containsExactlyElementsOf(profile);
    }

    @Test
    void shouldDownsampleWhenElevationPointsExceedDefaultMaxPoints() {
        // GIVEN
        List<ElevationPointDto> profile = IntStream.range(0, 300)
                .mapToObj(i -> new ElevationPointDto(i * 10.0, 100.0 + i))
                .toList();
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(new Route()));
        given(routeRepository.findElevationProfileById(ROUTE_ID)).willReturn(profile);

        // WHEN
        List<ElevationPointDto> result = routeService.getElevationPointsForRoute(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result).hasSizeLessThanOrEqualTo(250);
        assertThat(result.getFirst()).isEqualTo(profile.getFirst());
        assertThat(result.getLast()).isEqualTo(profile.getLast());
    }

    @Test
    void shouldGetRouteDetailSuccessfully() {
        // GIVEN
        Route mockRoute = new Route();
        RouteDetailDto expectedDto = RouteDetailDto.builder()
                .id(ROUTE_ID)
                .name(ROUTE_NAME)
                .build();

        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        given(routeMapper.toRouteDetailDto(mockRoute)).willReturn(expectedDto);

        // WHEN
        RouteDetailDto result = routeService.getRouteDetail(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(ROUTE_ID);
        assertThat(result.name()).isEqualTo(ROUTE_NAME);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingToNonExistentFolder() {
        // GIVEN
        Route mockRoute = new Route();
        UpdateRouteFolderDto request = new UpdateRouteFolderDto(FOLDER_ID);

        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        given(folderRepository.findByIdAndUserId(FOLDER_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.updateRouteFolder(ROUTE_ID, request, USER_ID))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Folder not found");

        assertThat(mockRoute.getFolder()).isNull();
    }

    @Test
    void shouldThrowRouteNotFoundExceptionWhenRenamingNonExistentRoute() {
        // GIVEN
        RenameRouteRequestDto request = new RenameRouteRequestDto("New name");
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.renameRoute(ROUTE_ID, request, USER_ID))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");

        then(routeRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldThrowRouteNotFoundExceptionWhenExportingNonExistentRouteToGpx() {
        // GIVEN
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.exportRouteToGpx(ROUTE_ID, USER_ID))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");

        then(gpxExportService).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowRouteNotFoundExceptionWhenGettingDetailForNonExistentRoute() {
        // GIVEN
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.getRouteDetail(ROUTE_ID, USER_ID))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");

        then(routeMapper).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowRouteNotFoundExceptionWhenGettingElevationPointsForNonExistentRoute() {
        // GIVEN
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.getElevationPointsForRoute(ROUTE_ID, USER_ID))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");
    }

    @Test
    void shouldReturnIntersectingZoneIdsFromRepository() {
        // GIVEN
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(new Route()));
        given(avoidanceZoneRepository.findIdsIntersectingRoute(ROUTE_ID, USER_ID)).willReturn(List.of(1L, 2L));

        // WHEN
        List<Long> result = routeService.getIntersectingZoneIds(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    void shouldThrowRouteNotFoundExceptionWhenGettingIntersectingZonesForNonExistentRoute() {
        // GIVEN
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.getIntersectingZoneIds(ROUTE_ID, USER_ID))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");

        then(avoidanceZoneRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowRouteNotFoundExceptionWhenUpdatingFolderForNonExistentRoute() {
        // GIVEN
        UpdateRouteFolderDto request = new UpdateRouteFolderDto(FOLDER_ID);
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> routeService.updateRouteFolder(ROUTE_ID, request, USER_ID))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");

        then(folderRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldResolveDefaultFileNameWhenRouteNameIsNull() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName(null);
        byte[] fakeGpxData = "<gpx>Route</gpx>".getBytes();
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        given(gpxExportService.generateGpxFile(mockRoute)).willReturn(fakeGpxData);

        // WHEN
        FileExportDto result = routeService.exportRouteToGpx(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result.fileName()).isEqualTo("route.gpx");
    }

    @Test
    void shouldResolveDefaultFileNameWhenRouteNameIsBlank() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName("   ");
        byte[] fakeGpxData = "<gpx>Route</gpx>".getBytes();
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        given(gpxExportService.generateGpxFile(mockRoute)).willReturn(fakeGpxData);

        // WHEN
        FileExportDto result = routeService.exportRouteToGpx(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result.fileName()).isEqualTo("route.gpx");
    }

    @Test
    void shouldSanitizeIllegalCharactersInExportFileName() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName("route/test:name");
        byte[] fakeGpxData = "<gpx>Route</gpx>".getBytes();
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        given(gpxExportService.generateGpxFile(mockRoute)).willReturn(fakeGpxData);

        // WHEN
        FileExportDto result = routeService.exportRouteToGpx(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result.fileName()).isEqualTo("route_test_name.gpx");
    }

    @Test
    void shouldResolveDefaultFileNameWhenRouteNameContainsOnlyUnderscores() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName("____");
        byte[] fakeGpxData = "<gpx>Route</gpx>".getBytes();
        given(routeRepository.findByIdAndUserId(ROUTE_ID, USER_ID)).willReturn(Optional.of(mockRoute));
        given(gpxExportService.generateGpxFile(mockRoute)).willReturn(fakeGpxData);

        // WHEN
        FileExportDto result = routeService.exportRouteToGpx(ROUTE_ID, USER_ID);

        // THEN
        assertThat(result.fileName()).isEqualTo("route.gpx");
    }
}
