package com.bogusz.bicyclerouteplannerbackend.dto.routing;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoundTripDto(
        @NotNull @Positive @Max(100_000) Integer length,
        @PositiveOrZero Integer seed
) {}
