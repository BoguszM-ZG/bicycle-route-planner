package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.telemetry.ClientLogRequestDto;
import com.bogusz.bicyclerouteplannerbackend.service.ClientTelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telemetry")
public class ClientTelemetryController {

    private final ClientTelemetryService telemetryService;

    @PostMapping("/logs")
    public ResponseEntity<Void> logClientEvent(@RequestBody ClientLogRequestDto logRequest) {
        telemetryService.processClientLog(logRequest);
        return ResponseEntity.accepted().build();
    }
}