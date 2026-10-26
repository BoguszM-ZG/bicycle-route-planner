package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.GpxExportException;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Length;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GpxExportServiceTest {

    private static final String ROUTE_NAME = "Test Route";
    private static final String CREATOR_NAME = "Bicycle Route Planner";

    private final GpxExportService gpxExportService = new GpxExportService();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    void shouldGenerateGpxFileSuccessfully() throws IOException {
        // Given
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(19.0, 50.0, 200.0),
                new Coordinate(19.01, 50.01, Double.NaN)
        };
        Route route = createRoute(ROUTE_NAME, coords);

        // When
        byte[] result = gpxExportService.generateGpxFile(route);

        // Then
        assertThat(result).isNotEmpty();
        String gpxContent = new String(result, StandardCharsets.UTF_8);
        assertThat(gpxContent)
                .contains("creator=\"" + CREATOR_NAME + "\"")
                .contains("<name>" + ROUTE_NAME + "</name>")
                .contains("<ele>200</ele>");

        GPX parsed = GPX.Reader.DEFAULT.read(new ByteArrayInputStream(result));
        assertThat(parsed.getCreator()).isEqualTo(CREATOR_NAME);
        assertThat(parsed.getTracks()).hasSize(1);

        Track track = parsed.getTracks().getFirst();
        assertThat(track.getName()).contains(ROUTE_NAME);
        TrackSegment segment = track.getSegments().getFirst();
        assertThat(segment.getPoints()).hasSize(2);
        assertThat(segment.getPoints().get(0).getLatitude().doubleValue()).isEqualTo(50.0);
        assertThat(segment.getPoints().get(0).getLongitude().doubleValue()).isEqualTo(19.0);
        assertThat(segment.getPoints().get(0).getElevation().map(Length::doubleValue)).contains(200.0);
        assertThat(segment.getPoints().get(1).getLatitude().doubleValue()).isEqualTo(50.01);
        assertThat(segment.getPoints().get(1).getLongitude().doubleValue()).isEqualTo(19.01);
        assertThat(segment.getPoints().get(1).getElevation()).isEmpty();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRouteIsNull() {
        // When & Then
        assertThatThrownBy(() -> gpxExportService.generateGpxFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Route must not be null");
    }

    @Test
    void shouldThrowGpxExportExceptionWhenRouteGeomIsNull() {
        // Given
        Route route = new Route();
        route.setName(ROUTE_NAME);
        route.setRouteGeom(null);

        // When & Then
        assertThatThrownBy(() -> gpxExportService.generateGpxFile(route))
                .isInstanceOf(GpxExportException.class)
                .hasMessage("Route geometry must not be null");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenOutputStreamIsNull() {
        // Given
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(19.0, 50.0),
                new Coordinate(19.01, 50.01)
        };
        Route route = createRoute(ROUTE_NAME, coords);

        // When & Then
        assertThatThrownBy(() -> gpxExportService.writeGpx(route, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OutputStream must not be null");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCoordinateIsNull() {
        // Given
        Coordinate[] coords = new Coordinate[]{
                null,
                new Coordinate(19.01, 50.01)
        };
        Route route = createRoute(ROUTE_NAME, coords);

        // When & Then
        assertThatThrownBy(() -> gpxExportService.generateGpxFile(route))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Coordinate must not be null");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCoordinateIsNonFinite() {
        // Given
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(Double.NaN, 50.0),
                new Coordinate(19.01, 50.01)
        };
        Route route = createRoute(ROUTE_NAME, coords);

        // When & Then
        assertThatThrownBy(() -> gpxExportService.generateGpxFile(route))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Coordinate latitude and longitude must be valid finite numbers");
    }


    private Route createRoute(String name, Coordinate[] coordinates) {
        Route route = new Route();
        route.setName(name);
        route.setRouteGeom(geometryFactory.createLineString(coordinates));
        return route;
    }
}
