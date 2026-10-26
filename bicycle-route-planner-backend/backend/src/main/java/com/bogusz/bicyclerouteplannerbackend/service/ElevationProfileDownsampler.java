package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.route.ElevationPointDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Component
public class ElevationProfileDownsampler {

    private static final int MIN_RETAINED_POINTS = 2;
    private static final int EXTREMA_PER_BUCKET = 2;
    private static final double BASELINE_DIVISOR = 2.0;

    public List<ElevationPointDto> downsample(List<ElevationPointDto> points, int maxPoints) {
        if (points == null || points.isEmpty()) {
            return Collections.emptyList();
        }

        int effectiveMaxPoints = Math.max(MIN_RETAINED_POINTS, maxPoints);
        if (points.size() <= effectiveMaxPoints) {
            return new ArrayList<>(points);
        }

        int intermediateCapacity = effectiveMaxPoints - MIN_RETAINED_POINTS;
        int bucketCount = intermediateCapacity / EXTREMA_PER_BUCKET;

        if (bucketCount <= 0) {
            return downsampleWithoutBuckets(points, effectiveMaxPoints, intermediateCapacity);
        }

        return downsampleWithBuckets(points, effectiveMaxPoints, bucketCount);
    }

    private List<ElevationPointDto> downsampleWithoutBuckets(
            List<ElevationPointDto> points,
            int effectiveMaxPoints,
            int intermediateCapacity
    ) {
        List<ElevationPointDto> result = new ArrayList<>(effectiveMaxPoints);
        result.add(points.getFirst());
        if (intermediateCapacity >= 1 && points.size() > MIN_RETAINED_POINTS) {
            int chosenIndex = findMostProminentExtremumIndex(points);
            result.add(points.get(chosenIndex));
        }
        result.add(points.getLast());
        return result;
    }

    private List<ElevationPointDto> downsampleWithBuckets(
            List<ElevationPointDto> points,
            int effectiveMaxPoints,
            int bucketCount
    ) {
        int intermediateCount = points.size() - MIN_RETAINED_POINTS;
        List<ElevationPointDto> result = new ArrayList<>(effectiveMaxPoints);
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

    private int findMostProminentExtremumIndex(List<ElevationPointDto> points) {
        int minIndex = 1;
        double minElevation = points.get(1).elevation();
        int maxIndex = 1;
        double maxElevation = points.get(1).elevation();

        int lastIntermediateIndex = points.size() - 1;
        for (int i = 2; i < lastIntermediateIndex; i++) {
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

        double baseline = (points.getFirst().elevation() + points.getLast().elevation()) / BASELINE_DIVISOR;
        double maxDiff = maxElevation - baseline;
        double minDiff = baseline - minElevation;

        return (maxDiff >= minDiff) ? maxIndex : minIndex;
    }
}
