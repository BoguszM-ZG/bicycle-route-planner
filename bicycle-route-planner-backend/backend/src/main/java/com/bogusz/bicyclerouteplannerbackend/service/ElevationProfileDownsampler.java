package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.route.ElevationPointDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class ElevationProfileDownsampler {

    private static final int RETAINED_ENDPOINTS = 2;
    private static final int EXTREMA_PER_BUCKET = 2;

    public List<ElevationPointDto> downsample(List<ElevationPointDto> points, int maxPoints) {
        if (points.size() <= maxPoints) {
            return points;
        }

        int bucketCount = (maxPoints - RETAINED_ENDPOINTS) / EXTREMA_PER_BUCKET;
        int intermediateCount = points.size() - RETAINED_ENDPOINTS;
        List<ElevationPointDto> result = new ArrayList<>(maxPoints);
        result.add(points.getFirst());

        for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
            int startIndex = 1 + (int) ((long) bucketIndex * intermediateCount / bucketCount);
            int endIndex = 1 + (int) ((long) (bucketIndex + 1) * intermediateCount / bucketCount);
            addBucketExtrema(points, startIndex, endIndex, result);
        }

        result.add(points.getLast());
        return result;
    }

    private void addBucketExtrema(
            List<ElevationPointDto> points,
            int startIndex,
            int endIndex,
            List<ElevationPointDto> result
    ) {
        int minIndex = startIndex;
        double minElevation = points.get(startIndex).elevation();
        int maxIndex = startIndex;
        double maxElevation = minElevation;

        for (int i = startIndex + 1; i < endIndex; i++) {
            double elevation = points.get(i).elevation();
            if (elevation < minElevation) {
                minElevation = elevation;
                minIndex = i;
            }
            if (elevation > maxElevation) {
                maxElevation = elevation;
                maxIndex = i;
            }
        }

        appendExtremaInOrder(points, minIndex, maxIndex, minElevation, maxElevation, result);
    }

    private void appendExtremaInOrder(
            List<ElevationPointDto> points,
            int minIndex,
            int maxIndex,
            double minElevation,
            double maxElevation,
            List<ElevationPointDto> result
    ) {
        if (minIndex == maxIndex || Double.compare(minElevation, maxElevation) == 0) {
            result.add(points.get(minIndex));
        } else if (minIndex < maxIndex) {
            result.add(points.get(minIndex));
            result.add(points.get(maxIndex));
        } else {
            result.add(points.get(maxIndex));
            result.add(points.get(minIndex));
        }
    }
}
