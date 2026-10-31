package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.GpxExportException;
import com.bogusz.bicyclerouteplannerbackend.model.Route;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class GpxExportService {

    public byte[] generateGpxFile(Route route) {
        List<WayPoint> wayPoints = Arrays.stream(route.getRouteGeom().getCoordinates())
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

        GPX gpx = GPX.builder()
                .creator("Bicycle Route Planner")
                .addTrack(Track.builder()
                        .name(route.getName())
                        .addSegment(TrackSegment.of(wayPoints))
                        .build())
                .build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            GPX.Writer.DEFAULT.write(gpx, outputStream);
        } catch (IOException e) {
            throw new GpxExportException("Error during GPX file generation", e);
        }
        return outputStream.toByteArray();
    }
}
