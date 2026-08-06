package com.bogusz.bicyclerouteplannerbackend.dto.auth;

import com.bogusz.bicyclerouteplannerbackend.dto.user.UserDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresInMs;
    private long refreshTokenExpiresInMs;
    private String tokenType;
    private UserDto user;

}

