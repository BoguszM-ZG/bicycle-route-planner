package com.bogusz.bicyclerouteplannerbackend.service;

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

    private Route createRoute(String name, Coordinate[] coordinates) {
        Route route = new Route();
        route.setName(name);
        route.setRouteGeom(geometryFactory.createLineString(coordinates));
        return route;
    }
}
