package com.bogusz.bicyclerouteplannerbackend.dto.auth;

import com.bogusz.bicyclerouteplannerbackend.dto.user.UserDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInMs,
        long refreshTokenExpiresInMs,
        String tokenType,
        UserDto user
) {}
