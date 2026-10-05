package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.telemetry.ClientLogRequestDto;
import com.bogusz.bicyclerouteplannerbackend.service.ClientTelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telemetry")
@Tag(name = "Telemetry", description = "Client-side logging and telemetry collection")
public class ClientTelemetryController {

    private final ClientTelemetryService telemetryService;

    @PostMapping("/logs")
    @Operation(summary = "Log client event", description = "Receives error logs and events from the frontend application.")
    public ResponseEntity<Void> logClientEvent(@RequestBody ClientLogRequestDto logRequest) {
        telemetryService.processClientLog(logRequest);
        return ResponseEntity.accepted().build();
    }
}