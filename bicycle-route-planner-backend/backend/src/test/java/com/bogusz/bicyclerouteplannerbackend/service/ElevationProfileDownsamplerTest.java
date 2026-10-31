package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.route.ElevationPointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ElevationProfileDownsamplerTest {
    private ElevationProfileDownsampler downsampler;

    @BeforeEach
    void setUp() {
        downsampler = new ElevationProfileDownsampler();
    }

    @Test
    void shouldReturnEmptyListWhenInputIsEmpty() {
        assertThat(downsampler.downsample(Collections.emptyList(), 250)).isEmpty();
    }

    @Test
    void shouldReturnOriginalPointsWhenCountLessThanOrEqualToMaxPoints() {
        List<ElevationPointDto> points = List.of(
                new ElevationPointDto(0.0, 100.0),
                new ElevationPointDto(100.0, 110.0),
                new ElevationPointDto(200.0, 120.0)
        );

        List<ElevationPointDto> result = downsampler.downsample(points, 5);

        assertThat(result).hasSize(3).containsExactlyElementsOf(points);
    }

    @Test
    void shouldHandleDuplicateDistancePoints() {
        List<ElevationPointDto> points = List.of(
                new ElevationPointDto(0.0, 100.0),
                new ElevationPointDto(0.0, 120.0),
                new ElevationPointDto(10.0, 300.0),
                new ElevationPointDto(10.0, 50.0),
                new ElevationPointDto(20.0, 150.0),
                new ElevationPointDto(20.0, 110.0)
        );

        List<ElevationPointDto> result = downsampler.downsample(points, 4);

        assertThat(result).hasSizeLessThanOrEqualTo(4);
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).distance()).isGreaterThanOrEqualTo(result.get(i - 1).distance());
        }
    }

    @Test
    void shouldPreserveInitialAndTerminalPoints() {
        List<ElevationPointDto> points = new ArrayList<>();
        points.add(new ElevationPointDto(0.0, 100.0));
        for (int i = 1; i <= 300; i++) {
            points.add(new ElevationPointDto(i * 10.0, 100.0 + (i % 50)));
        }
        points.add(new ElevationPointDto(1000.0 * 4.0, 120.0));

        List<ElevationPointDto> result = downsampler.downsample(points, 250);

        assertThat(result.getFirst().distance()).isEqualTo(0.0);
        assertThat(result.getFirst().elevation()).isEqualTo(100.0);
        assertThat(result.getLast().distance()).isEqualTo(1000.0 * 4.0);
        assertThat(result.getLast().elevation()).isEqualTo(120.0);
        assertThat(result).hasSizeLessThanOrEqualTo(250);
    }

    @Test
    void shouldPreserveExtremaWhenValleyPrecedesPeak() {
        List<ElevationPointDto> points = List.of(
                new ElevationPointDto(0.0, 100.0),
                new ElevationPointDto(10.0, 120.0),
                new ElevationPointDto(20.0, 50.0),
                new ElevationPointDto(30.0, 300.0),
                new ElevationPointDto(40.0, 130.0),
                new ElevationPointDto(50.0, 110.0)
        );

        List<ElevationPointDto> result = downsampler.downsample(points, 4);

        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo(points.get(0));
        assertThat(result.get(1)).isEqualTo(points.get(2));
        assertThat(result.get(2)).isEqualTo(points.get(3));
        assertThat(result.get(3)).isEqualTo(points.get(5));
    }

    @Test
    void shouldPreserveExtremaWhenPeakPrecedesValley() {
        List<ElevationPointDto> points = List.of(
                new ElevationPointDto(0.0, 100.0),
                new ElevationPointDto(10.0, 350.0),
                new ElevationPointDto(20.0, 40.0),
                new ElevationPointDto(30.0, 90.0),
                new ElevationPointDto(40.0, 100.0)
        );

        List<ElevationPointDto> result = downsampler.downsample(points, 4);

        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo(points.get(0));
        assertThat(result.get(1)).isEqualTo(points.get(1));
        assertThat(result.get(2)).isEqualTo(points.get(2));
        assertThat(result.get(3)).isEqualTo(points.get(4));
    }

    @Test
    void shouldEnsureResultPointsAreSortedByDistanceAscending() {
        List<ElevationPointDto> points = new ArrayList<>();
        double currentDistance = 0.0;
        for (int i = 0; i < 500; i++) {
            currentDistance += 12.5;
            double elevation = 100.0 + 50.0 * Math.sin(i * 0.1);
            points.add(new ElevationPointDto(currentDistance, elevation));
        }

        List<ElevationPointDto> result = downsampler.downsample(points, 250);

        assertThat(result).hasSizeLessThanOrEqualTo(250);
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).distance()).isGreaterThan(result.get(i - 1).distance());
        }
    }

    @Test
    void shouldHandleFlatElevationProfileInBuckets() {
        List<ElevationPointDto> points = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            points.add(new ElevationPointDto(i * 10.0, 150.0));
        }

        List<ElevationPointDto> result = downsampler.downsample(points, 6);

        assertThat(result).hasSizeLessThanOrEqualTo(6);
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).distance()).isGreaterThan(result.get(i - 1).distance());
            assertThat(result.get(i).elevation()).isEqualTo(150.0);
        }
    }

    @Test
    void shouldNotExceedMaxPointsThresholdForLargeRoute() {
        List<ElevationPointDto> largePoints = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            largePoints.add(new ElevationPointDto(i * 5.0, 200.0 + (i % 100)));
        }

        List<ElevationPointDto> result = downsampler.downsample(largePoints, 250);

        assertThat(result).hasSizeLessThanOrEqualTo(250);
        assertThat(result.getFirst()).isEqualTo(largePoints.getFirst());
        assertThat(result.getLast()).isEqualTo(largePoints.getLast());
    }
}
