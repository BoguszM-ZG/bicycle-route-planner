package com.bogusz.bicyclerouteplannerbackend.dto.routing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrsOptionsDto(
        @JsonProperty("avoid_polygons") @Valid AvoidPolygonsDto avoidPolygons,
        @JsonProperty("round_trip") @Valid RoundTripDto roundTrip
) {}
