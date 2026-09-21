package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.GpxExportException;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class GpxExportService {

    public byte[] generateGpxFile(Route route) {

        Coordinate[] coordinates = route.getRouteGeom().getCoordinates();

        List<WayPoint> wayPoints = Arrays.stream(coordinates)
                .map(coord -> {
                    WayPoint.Builder pointBuilder = WayPoint.builder()
                            .lat(coord.getY())
                            .lon(coord.getX());

                    if (!Double.isNaN(coord.getZ())) {
                        pointBuilder.ele(coord.getZ());
                    }
                    return pointBuilder.build();

                })
                .toList();

        TrackSegment segment = TrackSegment.of(wayPoints);

        GPX gpx = GPX.builder()
                .creator("Bicycle Route Planner")
                .addTrack(Track.builder()
                        .name(route.getName())
                        .addSegment(segment)
                        .build())
                .build();

        Path tempFilePath = null;
        try {
            tempFilePath = Files.createTempFile("route_", ".gpx");

            GPX.write(gpx, tempFilePath);

            return Files.readAllBytes(tempFilePath);

        } catch (IOException e) {
            throw new GpxExportException("Error during GPX file generation");
        } finally {
            if (tempFilePath != null) {
                try {
                    Files.deleteIfExists(tempFilePath);
                } catch (IOException e) {
                    log.error("Error while deleting temporary file: {}", tempFilePath, e);
                }
            }
        }
    }
}