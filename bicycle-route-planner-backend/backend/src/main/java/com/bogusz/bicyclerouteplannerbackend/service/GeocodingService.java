package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.ExternalGeocodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestClient photonRestClient;

    public String searchLocation(String query, Double lat, Double lon) {
        return photonRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/")
                            .queryParam("q", query)
                            .queryParam("limit", 5);
                    
                    if (lat != null && lon != null) {
                        uriBuilder.queryParam("lat", lat)
                                  .queryParam("lon", lon);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Error from Photon API. Status: {}, Body: {}", res.getStatusCode(), errorBody);
                    throw new ExternalGeocodingException("Failed to fetch geocoding data from external provider.");
                })
                .body(String.class);
    }
}
