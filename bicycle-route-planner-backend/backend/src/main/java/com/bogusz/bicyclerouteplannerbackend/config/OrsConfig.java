package com.bogusz.bicyclerouteplannerbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;


@Configuration
public class OrsConfig {

    @Value("${ors.api-url}")
    private String apiUrl;

    @Value("${ors.api-key}")
    private String apiKey;

    @Bean
    public RestClient orsRestClient() {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", apiKey)
                .defaultHeader("Accept", "application/json, application/geo+json")
                .build();
    }
}