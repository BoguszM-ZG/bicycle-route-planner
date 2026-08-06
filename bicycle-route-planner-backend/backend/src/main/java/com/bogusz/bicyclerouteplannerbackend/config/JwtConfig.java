package com.bogusz.bicyclerouteplannerbackend.config;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "application.security.jwt")
@Getter
@Setter
@Validated
public class JwtConfig {

    @NotBlank
    @Size(min = 32)
    private String secret;

    @Positive
    private long accessTokenExpirationMs;

    @Positive
    private long refreshTokenExpirationMs;

}
