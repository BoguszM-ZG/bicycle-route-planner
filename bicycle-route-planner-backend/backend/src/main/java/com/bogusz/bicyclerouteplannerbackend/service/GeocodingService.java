package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.geocoding.GeocodingSearchRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.ExternalGeocodingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestClient photonRestClient;

    public String searchLocation(GeocodingSearchRequestDto request) {
        return photonRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/")
                            .queryParam("q", request.q())
                            .queryParam("limit", 5);
                    
                    if (request.lat() != null && request.lon() != null) {
                        uriBuilder.queryParam("lat", request.lat())
                                  .queryParam("lon", request.lon());
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ExternalGeocodingException("Failed to fetch geocoding data from external provider (HTTP " + res.getStatusCode() + ")");
                })
                .body(String.class);
    }
}
