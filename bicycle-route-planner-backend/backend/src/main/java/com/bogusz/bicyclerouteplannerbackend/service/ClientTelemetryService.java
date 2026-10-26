package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.telemetry.ClientLogRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ClientTelemetryService {

    private static final Logger clientLog = LoggerFactory.getLogger("client.telemetry");
    private static final String LOG_TEMPLATE = "[CLIENT-SIDE] Time: {} | Msg: {} | Details: {}";

    @Async
    public void processClientLog(ClientLogRequestDto req) {
        if (req == null || req.level() == null) {
            return;
        }

        String safeTime = req.timestamp() != null ? sanitize(req.timestamp()) : Instant.now().toString();
        String safeMsg = sanitize(req.message());
        String safeDetails = req.details() != null ? sanitize(req.details()) : "";

        switch (req.level()) {
            case ERROR -> clientLog.error(LOG_TEMPLATE, safeTime, safeMsg, safeDetails);
            case WARN  -> clientLog.warn(LOG_TEMPLATE,  safeTime, safeMsg, safeDetails);
            case INFO  -> clientLog.info(LOG_TEMPLATE,  safeTime, safeMsg, safeDetails);
            case DEBUG -> clientLog.debug(LOG_TEMPLATE, safeTime, safeMsg, safeDetails);
        }
    }

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
