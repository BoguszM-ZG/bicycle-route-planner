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

    // No @Mock annotations are needed here, as the service has no external dependencies.
    // We can instantiate it directly.
    private final GpxExportService gpxExportService = new GpxExportService();

    private final GeometryFactory geometryFactory = new GeometryFactory();


    @Test
    void shouldGenerateGpxFileSuccessfully() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName("Test Route");

        // JTS requires at least 2 points to create a LineString
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(19.0, 50.0, 200.0), // X=lon, Y=lat, Z=elevation
                new Coordinate(19.01, 50.01, Double.NaN) // NaN means no elevation data
        };

        mockRoute.setRouteGeom(geometryFactory.createLineString(coords));

        // WHEN
        byte[] result = gpxExportService.generateGpxFile(mockRoute);

        // THEN
        assertThat(result).isNotEmpty();

        // Convert byte array to String to verify the internal XML structure of the GPX file
        String gpxContent = new String(result);

        assertThat(gpxContent).contains("creator=\"Bicycle Route Planner\"");
        assertThat(gpxContent).contains("<name>Test Route</name>");

        // Verify first point (with elevation). JPX removes .0 for whole numbers
        assertThat(gpxContent).contains("lat=\"50\" lon=\"19\"");
        assertThat(gpxContent).contains("<ele>200</ele>");

        // Verify second point (without elevation)
        assertThat(gpxContent).contains("lat=\"50.01\" lon=\"19.01\"");
    }


    @Test
    void shouldThrowExceptionWhenFileOperationsFail() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName("Error Route");

        mockRoute.setRouteGeom(geometryFactory.createLineString(
                new Coordinate[]{
                        new Coordinate(19.0, 50.0),
                        new Coordinate(19.01, 50.01)
                }
        ));

        // Mock the static Files.class to simulate a hard drive failure (e.g. out of disk space)
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            mockedFiles.when(() -> Files.createTempFile(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new IOException("Simulated Disk Full Exception"));

            // WHEN & THEN
            assertThatThrownBy(() -> gpxExportService.generateGpxFile(mockRoute))
                    .isInstanceOf(GpxExportException.class)
                    .hasMessageContaining("Error during GPX file generation");
        }
    }

    @Test
    void shouldNotThrowExceptionWhenDeletingTempFileFails() {
        // GIVEN
        Route mockRoute = new Route();
        mockRoute.setName("Temp File Error Route");

        mockRoute.setRouteGeom(geometryFactory.createLineString(
                new Coordinate[]{
                        new Coordinate(19.0, 50.0),
                        new Coordinate(19.01, 50.01)
                }
        ));

        // Using CALLS_REAL_METHODS ensures that createTempFile and readAllBytes work normally,
        // so the execution can successfully reach the finally block.
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {

            // Mock ONLY the deletion part to fail
            mockedFiles.when(() -> Files.deleteIfExists(Mockito.any(Path.class)))
                    .thenThrow(new IOException("Simulated Deletion Error"));

            // WHEN
            byte[] result = gpxExportService.generateGpxFile(mockRoute);

            // THEN
            // The method should catch the IOException, log it, and still return the file successfully
            assertThat(result).isNotEmpty();

            String gpxContent = new String(result);
            assertThat(gpxContent).contains("<name>Temp File Error Route</name>");
        }
    }
}