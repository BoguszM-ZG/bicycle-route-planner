package com.bogusz.bicyclerouteplannerbackend.dto.telemetry;

import com.bogusz.bicyclerouteplannerbackend.model.ClientLogLevel;

public record ClientLogRequestDto(
        ClientLogLevel level,
        String message,
        String timestamp,
        String details
) {
}
