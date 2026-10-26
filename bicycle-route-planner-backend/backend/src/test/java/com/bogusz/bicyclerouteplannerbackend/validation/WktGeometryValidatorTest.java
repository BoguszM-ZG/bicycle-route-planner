package com.bogusz.bicyclerouteplannerbackend.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class WktGeometryValidatorTest {
    private WktGeometryValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new WktGeometryValidator();
    }

    private void initValidator(GeometryType type) {
        ValidWktGeometry annotation = mock(ValidWktGeometry.class);
        given(annotation.expectedType()).willReturn(type);
        validator.initialize(annotation);
    }

    private void mockViolationBuilder() {
        given(context.buildConstraintViolationWithTemplate(anyString())).willReturn(violationBuilder);
    }

    @Test
    void shouldAcceptNullOrBlankWkt() {
        // GIVEN
        initValidator(GeometryType.POINT);

        // WHEN
        boolean nullValid = validator.isValid(null, context);
        boolean emptyValid = validator.isValid("", context);
        boolean blankValid = validator.isValid("   ", context);

        // THEN
        assertThat(nullValid).isTrue();
        assertThat(emptyValid).isTrue();
        assertThat(blankValid).isTrue();
    }

    @Test
    void shouldAcceptValidPoint() {
        // GIVEN
        initValidator(GeometryType.POINT);

        // WHEN
        boolean pointValid = validator.isValid("POINT(19.945 50.0647)", context);
        boolean point3DValid = validator.isValid("POINT(19.945 50.0647 250.0)", context);

        // THEN
        assertThat(pointValid).isTrue();
        assertThat(point3DValid).isTrue();
    }

    @Test
    void shouldRejectPointWhenLineStringExpected() {
        // GIVEN
        initValidator(GeometryType.LINE_STRING);
        mockViolationBuilder();

        // WHEN
        boolean isValid = validator.isValid("POINT(19.945 50.0647)", context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldAcceptValidLineString() {
        // GIVEN
        initValidator(GeometryType.LINE_STRING);

        // WHEN
        boolean isValid = validator.isValid("LINESTRING(19.945 50.0647, 19.946 50.0648)", context);

        // THEN
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectLineStringWithOnlyOnePoint() {
        // GIVEN
        initValidator(GeometryType.LINE_STRING);
        mockViolationBuilder();

        // WHEN
        boolean isValid = validator.isValid("LINESTRING(19.945 50.0647)", context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldAcceptValidPolygon() {
        // GIVEN
        initValidator(GeometryType.POLYGON);

        // WHEN
        boolean isValid = validator.isValid("POLYGON((19.9 50.0, 19.9 50.1, 20.0 50.1, 20.0 50.0, 19.9 50.0))", context);

        // THEN
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectSelfIntersectingPolygon() {
        // GIVEN
        initValidator(GeometryType.POLYGON);
        mockViolationBuilder();

        // WHEN
        boolean isValid = validator.isValid("POLYGON((0 0, 0 2, 2 0, 2 2, 0 0))", context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectCoordinatesOutOfBounds() {
        // GIVEN
        initValidator(GeometryType.POINT);
        mockViolationBuilder();

        // WHEN
        boolean latHighValid = validator.isValid("POINT(19.945 100.0)", context);
        boolean latLowValid = validator.isValid("POINT(19.945 -95.0)", context);
        boolean lonHighValid = validator.isValid("POINT(190.0 50.0)", context);
        boolean lonLowValid = validator.isValid("POINT(-185.0 50.0)", context);

        // THEN
        assertThat(latHighValid).isFalse();
        assertThat(latLowValid).isFalse();
        assertThat(lonHighValid).isFalse();
        assertThat(lonLowValid).isFalse();
    }

    @Test
    void shouldRejectEmptyGeometry() {
        // GIVEN
        initValidator(GeometryType.POINT);
        mockViolationBuilder();

        // WHEN
        boolean isValid = validator.isValid("POINT EMPTY", context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectMalformedWktSyntax() {
        // GIVEN
        initValidator(GeometryType.POINT);
        mockViolationBuilder();

        // WHEN
        boolean isValid = validator.isValid("INVALID_WKT(19.9)", context);

        // THEN
        assertThat(isValid).isFalse();
    }
}
