package com.bogusz.bicyclerouteplannerbackend.dto.geocoding;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeocodingSearchRequestDto(
        @NotBlank(message = "Search query is required")
        @Size(max = 200, message = "Search query cannot exceed 200 characters")
        String q,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
        Double lat,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
        Double lon
) {
}
