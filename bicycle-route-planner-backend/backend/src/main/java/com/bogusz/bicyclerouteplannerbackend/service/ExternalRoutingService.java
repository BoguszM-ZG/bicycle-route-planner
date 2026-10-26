package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.ExternalRoutingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ExternalRoutingService {

    private final RestClient orsRestClient;

    public String getDirections(OrsRouteRequestDto request) {
        return orsRestClient.post()
                .uri("/v2/directions/cycling-road/geojson")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ExternalRoutingException("Failed to calculate route: HTTP " + res.getStatusCode());
                })
                .body(String.class);
    }
}
