package com.bogusz.bicyclerouteplannerbackend.dto.telemetry;

import com.bogusz.bicyclerouteplannerbackend.model.ClientLogLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientLogRequestDto(
        @NotNull(message = "Log level is required")
        ClientLogLevel level,

        @NotBlank(message = "Log message is required")
        @Size(max = 500, message = "Log message cannot exceed 500 characters")
        String message,

        @Size(max = 100, message = "Timestamp cannot exceed 100 characters")
        String timestamp,

        @Size(max = 2000, message = "Details cannot exceed 2000 characters")
        String details
) {
}
