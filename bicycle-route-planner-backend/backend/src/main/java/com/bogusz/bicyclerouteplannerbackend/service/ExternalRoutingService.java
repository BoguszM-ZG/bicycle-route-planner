package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.routing.OrsRouteRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.ExternalRoutingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ExternalRoutingService {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final RestClient orsRestClient;

    public String getDirections(OrsRouteRequestDto request) {
        return orsRestClient.post()
                .uri("/v2/directions/cycling-road/geojson")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ExternalRoutingException("HTTP " + res.getStatusCode().value() + orsErrorCode(res));
                })
                .body(String.class);
    }

    private String orsErrorCode(ClientHttpResponse response) {
        try {
            JsonNode root = JSON_MAPPER.readTree(response.getBody());
            JsonNode code = root == null ? null : root.path("error").path("code");
            return code != null && code.isNumber() ? ", ORS code " + code.asInt() : "";
        } catch (IOException | JacksonException e) {
            return "";
        }
    }
}
