package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.telemetry.ClientLogRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class ClientTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(ClientTelemetryService.class);
    private static final String LOG_TEMPLATE = "[CLIENT-SIDE] Time: {} | Msg: {} | Details: {}";

    @Async
    public void processClientLog(ClientLogRequestDto req) {

        switch (req.level()) {
            case ERROR -> log.error(LOG_TEMPLATE, req.timestamp(), req.message(), req.details());
            case WARN  -> log.warn(LOG_TEMPLATE,  req.timestamp(), req.message(), req.details());
            case INFO  -> log.info(LOG_TEMPLATE,  req.timestamp(), req.message(), req.details());
            case DEBUG -> log.debug(LOG_TEMPLATE, req.timestamp(), req.message(), req.details());
        }
    }
}