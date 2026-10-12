package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.telemetry.ClientLogRequestDto;
import com.bogusz.bicyclerouteplannerbackend.model.ClientLogLevel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;

class ClientTelemetryServiceTest {

    // No dependencies to inject, pure functional service
    private final ClientTelemetryService telemetryService = new ClientTelemetryService();


    @ParameterizedTest
    @EnumSource(ClientLogLevel.class)
    void shouldProcessClientLogWithoutExceptionsForAnyLevel(ClientLogLevel level) {
        // Given
        ClientLogRequestDto request = new ClientLogRequestDto(
                level,
                "Test telemetry message",
                "2026-10-01T12:00:00Z",
                "App crashed on route calculation"
        );

        // When & Then
        assertThatCode(() -> telemetryService.processClientLog(request))
                .doesNotThrowAnyException();
    }
}