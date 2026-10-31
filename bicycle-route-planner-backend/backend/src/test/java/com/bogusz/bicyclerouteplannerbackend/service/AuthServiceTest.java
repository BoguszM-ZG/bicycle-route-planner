package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.LoginRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RefreshTokenRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.exception.RefreshTokenException;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String FAKE_ACCESS_TOKEN = "fake-access-token";
    private static final String FAKE_REFRESH_TOKEN = "fake-refresh-token";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 900000L;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604800000L;

    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor;

    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // GIVEN
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, "Test User");

        User mockSavedUser = new User();
        mockSavedUser.setId(TEST_USER_ID);
        mockSavedUser.setEmail(TEST_EMAIL);
        mockSavedUser.setFullName("Test User");

        given(userService.registerUser(TEST_EMAIL, TEST_PASSWORD, "Test User")).willReturn(mockSavedUser);
        mockTokenGeneration();

        // WHEN
        AuthResponse response = authService.register(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
        assertThat(response.accessTokenExpiresInMs()).isEqualTo(ACCESS_TOKEN_EXPIRATION_MS);
        assertThat(response.refreshTokenExpiresInMs()).isEqualTo(REFRESH_TOKEN_EXPIRATION_MS);
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(TEST_USER_ID);
        assertThat(response.user().email()).isEqualTo(TEST_EMAIL);
        assertThat(response.user().fullName()).isEqualTo("Test User");

        then(userService).should().registerUser(TEST_EMAIL, TEST_PASSWORD, "Test User");
    }

    @Test
    void shouldLoginUserSuccessfully() {
        // GIVEN
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        User mockUser = new User();
        mockUser.setId(TEST_USER_ID);
        mockUser.setEmail(TEST_EMAIL);

        Authentication mockAuthentication = mock(Authentication.class);

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(mockAuthentication);
        given(mockAuthentication.getPrincipal()).willReturn(UserPrincipal.fromUser(mockUser));
        mockTokenGeneration();

        // WHEN
        AuthResponse response = authService.login(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(TEST_USER_ID);
        assertThat(response.user().email()).isEqualTo(TEST_EMAIL);

        then(userService).shouldHaveNoInteractions();
        then(authenticationManager).should().authenticate(tokenCaptor.capture());
        UsernamePasswordAuthenticationToken capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getPrincipal()).isEqualTo(TEST_EMAIL);
        assertThat(capturedToken.getCredentials()).isEqualTo(TEST_PASSWORD);
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPrincipalIsNotUserPrincipalInstance() {
        // GIVEN
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        Authentication mockAuthentication = mock(Authentication.class);

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(mockAuthentication);
        given(mockAuthentication.getPrincipal()).willReturn("not-a-user-instance");

        // WHEN & THEN
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Unexpected authentication principal type");
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        // GIVEN
        RefreshTokenRequest request = new RefreshTokenRequest(FAKE_REFRESH_TOKEN);

        User mockUser = new User();
        mockUser.setId(TEST_USER_ID);
        mockUser.setEmail(TEST_EMAIL);

        given(jwtService.extractEmailFromRefreshToken(FAKE_REFRESH_TOKEN)).willReturn(TEST_EMAIL);
        given(userService.getByEmail(TEST_EMAIL)).willReturn(mockUser);
        mockTokenGeneration();

        // WHEN
        AuthResponse response = authService.refreshToken(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(TEST_USER_ID);
        assertThat(response.user().email()).isEqualTo(TEST_EMAIL);
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenForDisabledUser() {
        // GIVEN
        RefreshTokenRequest request = new RefreshTokenRequest(FAKE_REFRESH_TOKEN);

        User disabledUser = new User();
        disabledUser.setId(TEST_USER_ID);
        disabledUser.setEmail(TEST_EMAIL);
        disabledUser.setIsActive(false);

        given(jwtService.extractEmailFromRefreshToken(FAKE_REFRESH_TOKEN)).willReturn(TEST_EMAIL);
        given(userService.getByEmail(TEST_EMAIL)).willReturn(disabledUser);

        // WHEN & THEN
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("User account is disabled");
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringTokenRefresh() {
        // GIVEN
        RefreshTokenRequest request = new RefreshTokenRequest(FAKE_REFRESH_TOKEN);

        given(jwtService.extractEmailFromRefreshToken(FAKE_REFRESH_TOKEN)).willReturn(TEST_EMAIL);
        given(userService.getByEmail(TEST_EMAIL)).willThrow(new UsernameNotFoundException("User not found"));

        // WHEN & THEN
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("User not found or refresh token invalid");
    }

    @Test
    void shouldPropagateExceptionWhenJwtServiceFailsToExtractEmail() {
        // GIVEN
        RefreshTokenRequest request = new RefreshTokenRequest("MALFORMED_TOKEN");
        given(jwtService.extractEmailFromRefreshToken("MALFORMED_TOKEN"))
                .willThrow(new RefreshTokenException("Invalid refresh token"));

        // WHEN & THEN
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid refresh token");
    }

    private void mockTokenGeneration() {
        given(jwtService.generateAccessToken(any(UserPrincipal.class))).willReturn(FAKE_ACCESS_TOKEN);
        given(jwtService.generateRefreshToken(any(UserPrincipal.class))).willReturn(FAKE_REFRESH_TOKEN);
        given(jwtService.getAccessTokenExpirationMs()).willReturn(ACCESS_TOKEN_EXPIRATION_MS);
        given(jwtService.getRefreshTokenExpirationMs()).willReturn(REFRESH_TOKEN_EXPIRATION_MS);
    }
}
