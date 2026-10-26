package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.telemetry.ClientLogRequestDto;
import com.bogusz.bicyclerouteplannerbackend.service.ClientTelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telemetry")
@Tag(name = "Telemetry", description = "Client-side logging and telemetry collection")
public class ClientTelemetryController {

    private final ClientTelemetryService telemetryService;

    @PostMapping("/logs")
    @Operation(summary = "Log client event", description = "Receives error logs and events from the frontend application.")
    public ResponseEntity<Void> logClientEvent(@Valid @RequestBody ClientLogRequestDto logRequest) {
        telemetryService.processClientLog(logRequest);
        return ResponseEntity.accepted().build();
    }
}
