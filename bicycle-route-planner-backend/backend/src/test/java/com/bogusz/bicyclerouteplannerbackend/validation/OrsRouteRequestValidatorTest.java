package com.bogusz.bicyclerouteplannerbackend.validation;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrsRouteRequestValidatorTest {
    private OrsRouteRequestValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        validator = new OrsRouteRequestValidator();
    }

    private void mockViolationBuilder() {
        given(context.buildConstraintViolationWithTemplate(anyString())).willReturn(violationBuilder);
        given(violationBuilder.addPropertyNode(anyString())).willReturn(nodeBuilder);
    }

    @Test
    void shouldAcceptValidDirectRouteWithTwoCoordinates() {
        // GIVEN
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(19.0, 52.0), List.of(19.1, 52.1)),
                true, true, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldAcceptValidDirectRouteWithThreeDimensions() {
        // GIVEN
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(19.0, 52.0, 100.0), List.of(19.1, 52.1, 120.0)),
                true, true, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldAcceptValidRoundTripWithSingleCoordinate() {
        // GIVEN
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(19.0, 52.0)),
                true, false, Map.of("round_trip", Map.of("length", 15000))
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectDirectRouteWithOnlyOneCoordinate() {
        // GIVEN
        mockViolationBuilder();
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(19.0, 52.0)),
                true, false, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectEmptyCoordinates() {
        // GIVEN
        mockViolationBuilder();
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(),
                true, false, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectCoordinateWithOutOfBoundsLongitude() {
        // GIVEN
        mockViolationBuilder();
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(190.0, 52.0), List.of(19.1, 52.1)),
                true, false, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectCoordinateWithOutOfBoundsLatitude() {
        // GIVEN
        mockViolationBuilder();
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(19.0, -95.0), List.of(19.1, 52.1)),
                true, false, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectCoordinateWithLessThanTwoDimensions() {
        // GIVEN
        mockViolationBuilder();
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(19.0), List.of(19.1, 52.1)),
                true, false, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectCoordinateWithMoreThanThreeDimensions() {
        // GIVEN
        mockViolationBuilder();
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(19.0, 52.0, 100.0, 5.0), List.of(19.1, 52.1)),
                true, false, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectCoordinateWithNanValues() {
        // GIVEN
        mockViolationBuilder();
        OrsRouteRequestDto dto = new OrsRouteRequestDto(
                List.of(List.of(Double.NaN, 52.0), List.of(19.1, 52.1)),
                true, false, null
        );

        // WHEN
        boolean isValid = validator.isValid(dto, context);

        // THEN
        assertThat(isValid).isFalse();
    }
}
