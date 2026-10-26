package com.bogusz.bicyclerouteplannerbackend.validation;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class OrsRouteRequestValidator implements ConstraintValidator<ValidOrsRouteRequest, OrsRouteRequestDto> {

    private static final String COORDINATES_PROPERTY = "coordinates";
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final int MIN_COORDINATE_DIMENSIONS = 2;
    private static final int MAX_COORDINATE_DIMENSIONS = 3;
    private static final String ROUND_TRIP_KEY = "round_trip";

    @Override
    public boolean isValid(OrsRouteRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        List<List<Double>> coordinates = dto.coordinates();
        if (!hasValidRouteSize(dto, coordinates, context)) {
            return false;
        }

        return validateAllPoints(coordinates, context);
    }

    private boolean hasValidRouteSize(OrsRouteRequestDto dto, List<List<Double>> coordinates, ConstraintValidatorContext context) {
        if (coordinates == null || coordinates.isEmpty()) {
            return buildCoordinateViolation(context, "Coordinates list cannot be empty");
        }

        boolean isRoundTrip = dto.options() != null && dto.options().containsKey(ROUND_TRIP_KEY);
        if (!isRoundTrip && coordinates.size() < 2) {
            return buildCoordinateViolation(context, "At least two coordinates are required for direct routing");
        }

        return true;
    }

    private boolean validateAllPoints(List<List<Double>> coordinates, ConstraintValidatorContext context) {
        for (int i = 0; i < coordinates.size(); i++) {
            List<Double> point = coordinates.get(i);
            if (!isValidCoordinatePoint(point, i, context)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidCoordinatePoint(List<Double> point, int index, ConstraintValidatorContext context) {
        if (point == null || point.size() < MIN_COORDINATE_DIMENSIONS || point.size() > MAX_COORDINATE_DIMENSIONS) {
            return buildCoordinateViolation(context,
                    String.format("Coordinate at index %d must contain [longitude, latitude] or [longitude, latitude, elevation]", index));
        }

        Double lon = point.get(0);
        Double lat = point.get(1);
        if (isInvalidNumber(lon) || isInvalidNumber(lat)) {
            return buildCoordinateViolation(context,
                    String.format("Coordinate values at index %d must be valid non-null numbers", index));
        }

        return validateBounds(lon, lat, index, context);
    }

    private boolean isInvalidNumber(Double value) {
        return value == null || Double.isNaN(value) || Double.isInfinite(value);
    }

    private boolean validateBounds(Double lon, Double lat, int index, ConstraintValidatorContext context) {
        if (lon < MIN_LONGITUDE || lon > MAX_LONGITUDE) {
            return buildCoordinateViolation(context,
                    String.format("Longitude %f at index %d is out of bounds [%.1f, %.1f]", lon, index, MIN_LONGITUDE, MAX_LONGITUDE));
        }

        if (lat < MIN_LATITUDE || lat > MAX_LATITUDE) {
            return buildCoordinateViolation(context,
                    String.format("Latitude %f at index %d is out of bounds [%.1f, %.1f]", lat, index, MIN_LATITUDE, MAX_LATITUDE));
        }

        return true;
    }

    private boolean buildCoordinateViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(COORDINATES_PROPERTY)
                .addConstraintViolation();
        return false;
    }
}
