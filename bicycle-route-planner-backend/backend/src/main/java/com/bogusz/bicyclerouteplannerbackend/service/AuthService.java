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
        String refreshToken = request.refreshToken();
        String userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail != null) {
            User user = userService.getByEmail(userEmail);

            if (jwtService.isTokenValid(refreshToken)) {
                return createAuthResponse(user);
            }
        }
        throw new RefreshTokenException("Invalid refresh token");
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
