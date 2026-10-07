package com.bogusz.bicyclerouteplannerbackend.integration;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.LoginRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RefreshTokenRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class AuthIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_EMAIL = "integration@test.com";
    private static final String TEST_PASSWORD = "HardPassword123!";
    private static final String TEST_WRONG_PASSWORD = "WrongPassword123!";
    private static final String TEST_FULL_NAME = "Integration Test User";
    private static final String TEST_INVALID_TOKEN = "invalid.jwt.token";
    
    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String REFRESH_URL = "/api/auth/refresh";
    
    private static final String ERROR_USER_ALREADY_EXISTS = "User already exists";
    private static final String ERROR_AUTHORIZATION = "Authorization Error";
    private static final String ERROR_TOKEN_REFRESH = "Token Refresh Error";

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRegisterNewUserAndSaveToRealDatabase() throws Exception {
        // Given
        RegisterRequest request = createRegisterRequest();
        assertThat(userRepository.findByEmail(TEST_EMAIL)).isEmpty();

        // When
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL));

        // Then
        assertThat(userRepository.findByEmail(TEST_EMAIL)).isPresent();
    }

    @Test
    void shouldReturn409ConflictWhenRegisteringWithExistingEmail() throws Exception {
        // Given
        RegisterRequest request = createRegisterRequest();
        registerUserViaApi(request);

        // When
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                // Then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value(ERROR_USER_ALREADY_EXISTS));
    }


    @Test
    void shouldReturn200AndTokensWhenLoggingInWithValidCredentials() throws Exception {
        // Given
        RegisterRequest registerRequest = createRegisterRequest();
        registerUserViaApi(registerRequest);

        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        // When
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(loginRequest)))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL));
    }

    @Test
    void shouldReturn401UnauthorizedWhenLoggingInWithInvalidPassword() throws Exception {
        // Given
        RegisterRequest registerRequest = createRegisterRequest();
        registerUserViaApi(registerRequest);

        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_WRONG_PASSWORD);

        // When
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(loginRequest)))
                // Then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value(ERROR_AUTHORIZATION));
    }

    @Test
    void shouldReturn200AndNewTokensWhenRefreshingWithValidToken() throws Exception {
        // Given
        RegisterRequest registerRequest = createRegisterRequest();
        String refreshToken = registerUserAndGetRefreshToken(registerRequest);
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        // When
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(refreshRequest)))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void shouldReturnErrorWhenRefreshingWithInvalidToken() throws Exception {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(TEST_INVALID_TOKEN);

        // When
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(refreshRequest)))
                // Then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value(ERROR_TOKEN_REFRESH));
    }

    @Test
    void shouldReturnErrorWhenRefreshingWithAccessToken() throws Exception {
        // Given
        RegisterRequest registerRequest = createRegisterRequest();
        String responseContent = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String accessToken = jsonMapper.readValue(responseContent, AuthResponse.class).accessToken();
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(accessToken);

        // When
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(refreshRequest)))
                // Then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value(ERROR_TOKEN_REFRESH));
    }


    private RegisterRequest createRegisterRequest() {
        return new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);
    }

    private void registerUserViaApi(RegisterRequest request) throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String registerUserAndGetRefreshToken(RegisterRequest request) throws Exception {
        String responseContent = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, AuthResponse.class).refreshToken();
    }
}
