package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.LoginRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RefreshTokenRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.exception.RefreshTokenException;
import com.bogusz.bicyclerouteplannerbackend.mapper.UserMapper;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthResponse register(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = userService.registerUser(request.getEmail(), encodedPassword, request.getFullName());

        log.info("Authentication response generated for new user with ID: {}", user.getId());
        return createAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = Objects.requireNonNull((User) authentication.getPrincipal());
        log.info("User logged in successfully with ID: {}", user.getId());
        return createAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new RefreshTokenException("Invalid refresh token");
        }

        String refreshToken = request.refreshToken();
        String userEmail;

        try {
            io.jsonwebtoken.Claims claims = jwtService.extractAllClaims(refreshToken);
            if (!"REFRESH".equals(claims.get("type", String.class))) {
                log.warn("Attempt to use non-refresh token as a refresh token");
                throw new RefreshTokenException("Invalid token type");
            }
            userEmail = claims.getSubject();
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.warn("Failed to parse refresh token: {}", e.getMessage());
            throw new RefreshTokenException("Invalid refresh token");
        }

        if (userEmail == null) {
            throw new RefreshTokenException("Invalid refresh token");
        }

        User user = userService.getByEmail(userEmail);
        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                jwtService.getRefreshTokenExpirationMs(),
                "Bearer",
                userMapper.toDto(user)
        );
    }
}