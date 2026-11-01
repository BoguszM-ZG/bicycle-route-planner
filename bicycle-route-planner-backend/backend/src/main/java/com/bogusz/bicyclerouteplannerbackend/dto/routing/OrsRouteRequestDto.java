package com.bogusz.bicyclerouteplannerbackend.dto.routing;

import com.bogusz.bicyclerouteplannerbackend.validation.ValidOrsRouteRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

@ValidOrsRouteRequest
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrsRouteRequestDto(
        @Size(max = 50) List<List<Double>> coordinates,
        Boolean elevation,
        Boolean instructions,
        @Valid OrsOptionsDto options
) {}
