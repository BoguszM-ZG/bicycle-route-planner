package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.GpxExportException;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class GpxExportService {

    public byte[] generateGpxFile(Route route) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeGpx(route, outputStream);
        return outputStream.toByteArray();
    }

    public void writeGpx(Route route, OutputStream outputStream) {
        if (route == null) {
            throw new IllegalArgumentException("Route must not be null");
        }
        if (route.getRouteGeom() == null) {
            throw new GpxExportException("Route geometry must not be null");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("OutputStream must not be null");
        }

        Coordinate[] coordinates = route.getRouteGeom().getCoordinates();

        List<WayPoint> wayPoints = Arrays.stream(coordinates)
                .map(coord -> {
                    if (coord == null) {
                        throw new IllegalArgumentException("Coordinate must not be null");
                    }
                    if (!Double.isFinite(coord.getY()) || !Double.isFinite(coord.getX())) {
                        throw new IllegalArgumentException("Coordinate latitude and longitude must be valid finite numbers");
                    }
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

        try {
            GPX.Writer.DEFAULT.write(gpx, outputStream);
        } catch (IOException e) {
            throw new GpxExportException("Error during GPX file generation", e);
        }
    }
}
