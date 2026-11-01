package com.bogusz.bicyclerouteplannerbackend.dto.routing;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AvoidPolygonsDto(@NotEmpty List<List<List<List<Double>>>> coordinates) {

    @JsonProperty(value = "type", access = JsonProperty.Access.READ_ONLY)
    public String type() {
        return "MultiPolygon";
    }
}
