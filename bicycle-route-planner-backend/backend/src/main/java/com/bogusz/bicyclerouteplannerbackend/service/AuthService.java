package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.LoginRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RefreshTokenRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.user.UserDto;
import com.bogusz.bicyclerouteplannerbackend.exception.RefreshTokenException;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthResponse register(RegisterRequest request) {
        User user = userService.registerUser(request.email(), request.password(), request.fullName());
        return createAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadCredentialsException("Unexpected authentication principal type");
        }
        log.info("User logged in successfully with ID: {}", principal.getId());
        return createAuthResponse(principal);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new RefreshTokenException("Invalid refresh token");
        }

        String userEmail = jwtService.extractEmailFromRefreshToken(request.refreshToken());
        User user;
        try {
            user = userService.getByEmail(userEmail);
        } catch (UsernameNotFoundException e) {
            throw new RefreshTokenException("User not found or refresh token invalid", e);
        }
        if (!user.isEnabled()) {
            throw new RefreshTokenException("User account is disabled");
        }
        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(UserPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                jwtService.getRefreshTokenExpirationMs(),
                "Bearer",
                UserDto.from(principal)
        );
    }

    private AuthResponse createAuthResponse(User user) {
        return createAuthResponse(UserPrincipal.fromUser(user));
    }
}
