package com.bogusz.bicyclerouteplannerbackend.dto.routing;

import com.bogusz.bicyclerouteplannerbackend.validation.ValidOrsRouteRequest;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@ValidOrsRouteRequest
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrsRouteRequestDto(
        List<List<Double>> coordinates,
        Boolean elevation,
        Boolean instructions,
        Map<String, Object> options
) {}
