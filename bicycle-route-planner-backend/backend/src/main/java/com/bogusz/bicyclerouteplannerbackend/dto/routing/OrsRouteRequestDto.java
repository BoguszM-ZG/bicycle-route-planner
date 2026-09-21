package com.bogusz.bicyclerouteplannerbackend.dto.routing;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrsRouteRequestDto(
        List<List<Double>> coordinates,
        Boolean elevation,
        Boolean instructions,
        Map<String, Object> options
) {
}
