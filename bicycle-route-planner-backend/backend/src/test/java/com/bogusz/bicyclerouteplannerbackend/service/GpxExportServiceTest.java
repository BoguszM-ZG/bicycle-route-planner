package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.GpxExportException;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GpxExportServiceTest {

    private static final String TEST_ROUTE_NAME = "Test Route";
    private static final String ERROR_ROUTE_NAME = "Error Route";
    private static final String TEMP_FILE_ERROR_ROUTE_NAME = "Temp File Error Route";
    
    private static final double LON_1 = 19.0;
    private static final double LAT_1 = 50.0;
    private static final double ELEVATION_1 = 200.0;
    
    private static final double LON_2 = 19.01;
    private static final double LAT_2 = 50.01;

    private static final String CREATOR_STRING = "creator=\"Bicycle Route Planner\"";
    private static final String SIMULATED_DISK_ERROR = "Simulated Disk Full Exception";
    private static final String SIMULATED_DELETE_ERROR = "Simulated Deletion Error";
    private static final String GENERATION_ERROR_MSG = "Error during GPX file generation";

    private final GpxExportService gpxExportService = new GpxExportService();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    void shouldGenerateGpxFileSuccessfully() {
        // Given
        Route mockRoute = new Route();
        mockRoute.setName(TEST_ROUTE_NAME);

        Coordinate[] coords = new Coordinate[]{
                new Coordinate(LON_1, LAT_1, ELEVATION_1),
                new Coordinate(LON_2, LAT_2, Double.NaN)
        };

        mockRoute.setRouteGeom(geometryFactory.createLineString(coords));

        // When
        byte[] result = gpxExportService.generateGpxFile(mockRoute);

        // Then
        assertThat(result).isNotEmpty();

        String gpxContent = new String(result);

        assertThat(gpxContent).contains(CREATOR_STRING);
        assertThat(gpxContent).contains("<name>" + TEST_ROUTE_NAME + "</name>");

        assertThat(gpxContent).contains("lat=\"50\" lon=\"19\"");
        assertThat(gpxContent).contains("<ele>200</ele>");

        assertThat(gpxContent).contains("lat=\"50.01\" lon=\"19.01\"");
    }

    @Test
    void shouldThrowExceptionWhenFileOperationsFail() {
        // Given
        Route mockRoute = new Route();
        mockRoute.setName(ERROR_ROUTE_NAME);

        mockRoute.setRouteGeom(geometryFactory.createLineString(
                new Coordinate[]{
                        new Coordinate(LON_1, LAT_1),
                        new Coordinate(LON_2, LAT_2)
                }
        ));

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            mockedFiles.when(() -> Files.createTempFile(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new IOException(SIMULATED_DISK_ERROR));

            // When & THEN
            assertThatThrownBy(() -> gpxExportService.generateGpxFile(mockRoute))
                    .isInstanceOf(GpxExportException.class)
                    .hasMessageContaining(GENERATION_ERROR_MSG);
        }
    }

    @Test
    void shouldNotThrowExceptionWhenDeletingTempFileFails() {
        // Given
        Route mockRoute = new Route();
        mockRoute.setName(TEMP_FILE_ERROR_ROUTE_NAME);

        mockRoute.setRouteGeom(geometryFactory.createLineString(
                new Coordinate[]{
                        new Coordinate(LON_1, LAT_1),
                        new Coordinate(LON_2, LAT_2)
                }
        ));

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {

            mockedFiles.when(() -> Files.deleteIfExists(Mockito.any(Path.class)))
                    .thenThrow(new IOException(SIMULATED_DELETE_ERROR));

            // When
            byte[] result = gpxExportService.generateGpxFile(mockRoute);

            // Then
            assertThat(result).isNotEmpty();

            String gpxContent = new String(result);
            assertThat(gpxContent).contains("<name>" + TEMP_FILE_ERROR_ROUTE_NAME + "</name>");
        }
    }
}