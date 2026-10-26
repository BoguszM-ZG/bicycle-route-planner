package com.bogusz.bicyclerouteplannerbackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class WktGeometryValidator implements ConstraintValidator<ValidWktGeometry, String> {

    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final int MIN_LINESTRING_POINTS = 2;

    private GeometryType expectedType;

    @Override
    public void initialize(ValidWktGeometry constraintAnnotation) {
        this.expectedType = constraintAnnotation.expectedType();
    }

    @Override
    public boolean isValid(String wktValue, ConstraintValidatorContext context) {
        if (wktValue == null || wktValue.isBlank()) {
            return true;
        }

        Geometry geometry;
        try {
            WKTReader reader = new WKTReader();
            geometry = reader.read(wktValue);
        } catch (ParseException | IllegalArgumentException e) {
            return buildViolation(context, "Invalid WKT syntax: " + e.getMessage());
        }

        if (geometry == null || geometry.isEmpty()) {
            return buildViolation(context, "Geometry cannot be empty");
        }

        if (!matchesExpectedType(geometry)) {
            return buildViolation(context, "Geometry must be of type " + expectedType);
        }

        if (!validateCoordinates(geometry, context)) {
            return false;
        }

        if (!geometry.isValid()) {
            return buildViolation(context, "Geometry topology is invalid (e.g. self-intersecting polygon)");
        }

        return true;
    }

    private boolean matchesExpectedType(Geometry geometry) {
        return switch (expectedType) {
            case POINT -> geometry instanceof Point;
            case LINE_STRING -> geometry instanceof LineString lineString && lineString.getNumPoints() >= MIN_LINESTRING_POINTS;
            case POLYGON -> geometry instanceof Polygon;
            case ANY -> true;
        };
    }

    private boolean validateCoordinates(Geometry geometry, ConstraintValidatorContext context) {
        Coordinate[] coordinates = geometry.getCoordinates();
        for (Coordinate coord : coordinates) {
            if (Double.isNaN(coord.getX()) || Double.isNaN(coord.getY())
                    || Double.isInfinite(coord.getX()) || Double.isInfinite(coord.getY())) {
                return buildViolation(context, "Coordinates must not be NaN or Infinite");
            }

            if (coord.getX() < MIN_LONGITUDE || coord.getX() > MAX_LONGITUDE) {
                return buildViolation(context, String.format(
                        "Longitude %f is out of bounds [%.1f, %.1f]", coord.getX(), MIN_LONGITUDE, MAX_LONGITUDE));
            }

            if (coord.getY() < MIN_LATITUDE || coord.getY() > MAX_LATITUDE) {
                return buildViolation(context, String.format(
                        "Latitude %f is out of bounds [%.1f, %.1f]", coord.getY(), MIN_LATITUDE, MAX_LATITUDE));
            }
        }
        return true;
    }

    private boolean buildViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
