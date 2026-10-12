package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.LoginRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RefreshTokenRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.service.AuthService;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String EMAIL = "test@example.com";
    private static final String INVALID_EMAIL = "invalid-email";
    private static final String BLANK_STRING = "";
    private static final String PASSWORD = "StrongPassword123!";
    private static final String FULL_NAME = "Test User";
    private static final String ACCESS_TOKEN = "access.token.string";
    private static final String REFRESH_TOKEN = "refresh.token.string";
    private static final long EXPIRES_IN_MS = 3600000L;
    private static final String TOKEN_TYPE = "Bearer";

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String REFRESH_URL = "/api/auth/refresh";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturn201WhenRegisteringValidUser() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, FULL_NAME);
        AuthResponse response = createAuthResponse();

        given(authService.register(any())).willReturn(response);

        // When
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.tokenType").value(TOKEN_TYPE));

        // Then
        ArgumentCaptor<RegisterRequest> registerCaptor = ArgumentCaptor.forClass(RegisterRequest.class);
        then(authService).should(times(1)).register(registerCaptor.capture());
        
        RegisterRequest capturedRequest = registerCaptor.getValue();
        assertThat(capturedRequest.email()).isEqualTo(EMAIL);
        assertThat(capturedRequest.password()).isEqualTo(PASSWORD);
        assertThat(capturedRequest.fullName()).isEqualTo(FULL_NAME);
    }

    @Test
    void shouldReturn400BadRequestWhenRegisteringWithInvalidEmail() throws Exception {
        // Given
        RegisterRequest badRequest = new RegisterRequest(INVALID_EMAIL, PASSWORD, FULL_NAME);

        // When & Then
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(authService).should(never()).register(any());
    }

    @Test
    void shouldReturn200WhenLoggingInValidUser() throws Exception {
        // Given
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        AuthResponse response = createAuthResponse();

        given(authService.login(any())).willReturn(response);

        // When
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.tokenType").value(TOKEN_TYPE));

        // Then
        ArgumentCaptor<LoginRequest> loginCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        then(authService).should(times(1)).login(loginCaptor.capture());
        
        LoginRequest capturedRequest = loginCaptor.getValue();
        assertThat(capturedRequest.email()).isEqualTo(EMAIL);
        assertThat(capturedRequest.password()).isEqualTo(PASSWORD);
    }

    @Test
    void shouldReturn400BadRequestWhenLoggingInWithBlankPassword() throws Exception {
        // Given
        LoginRequest badRequest = new LoginRequest(EMAIL, BLANK_STRING);

        // When & Then
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(authService).should(never()).login(any());
    }

    @Test
    void shouldReturn200WhenRefreshingToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
        AuthResponse response = createAuthResponse();

        given(authService.refreshToken(any())).willReturn(response);

        // When
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN));

        // Then
        ArgumentCaptor<RefreshTokenRequest> refreshCaptor = ArgumentCaptor.forClass(RefreshTokenRequest.class);
        then(authService).should(times(1)).refreshToken(refreshCaptor.capture());
        
        RefreshTokenRequest capturedRequest = refreshCaptor.getValue();
        assertThat(capturedRequest.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    void shouldReturn400BadRequestWhenRefreshingWithBlankToken() throws Exception {
        // Given
        RefreshTokenRequest badRequest = new RefreshTokenRequest(BLANK_STRING);

        // When & Then
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(authService).should(never()).refreshToken(any());
    }

    private AuthResponse createAuthResponse() {
        return new AuthResponse(
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                EXPIRES_IN_MS,
                EXPIRES_IN_MS,
                TOKEN_TYPE,
                null
        );
    }
}
