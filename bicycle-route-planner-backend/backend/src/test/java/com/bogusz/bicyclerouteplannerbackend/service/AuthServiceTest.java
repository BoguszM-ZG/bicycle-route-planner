package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.LoginRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RefreshTokenRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.user.UserDto;
import com.bogusz.bicyclerouteplannerbackend.exception.RefreshTokenException;
import com.bogusz.bicyclerouteplannerbackend.mapper.UserMapper;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Test123";
    private static final String TEST_ENCODED_PASSWORD = "encodedPassword!@#";
    private static final String TEST_FULL_NAME = "Test User";
    private static final String FAKE_ACCESS_TOKEN = "fake-access-token";
    private static final String FAKE_REFRESH_TOKEN = "fake-refresh-token";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 3600000L;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 86400000L;

    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor;

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;


    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // GIVEN
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);

        User mockSavedUser = new User();
        mockSavedUser.setId(1L);
        mockSavedUser.setEmail(TEST_EMAIL);
        mockSavedUser.setFullName(TEST_FULL_NAME);

        UserDto mockUserDto = UserDto.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .fullName(TEST_FULL_NAME)
                .build();

        given(passwordEncoder.encode(TEST_PASSWORD)).willReturn(TEST_ENCODED_PASSWORD);
        given(userService.registerUser(TEST_EMAIL, TEST_ENCODED_PASSWORD, TEST_FULL_NAME)).willReturn(mockSavedUser);
        mockTokenGeneration(mockSavedUser, mockUserDto);

        // WHEN
        AuthResponse response = authService.register(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
        assertThat(response.accessTokenExpiresInMs()).isEqualTo(ACCESS_TOKEN_EXPIRATION_MS);
        assertThat(response.refreshTokenExpiresInMs()).isEqualTo(REFRESH_TOKEN_EXPIRATION_MS);

        // VERIFY
        then(userService).should().registerUser(TEST_EMAIL, TEST_ENCODED_PASSWORD, TEST_FULL_NAME);
    }

    @Test
    void shouldLoginUserSuccessfully() {
        // GIVEN
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail(TEST_EMAIL);

        UserDto mockUserDto = UserDto.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .build();
        Authentication mockAuthentication = mock(Authentication.class);

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(mockAuthentication);
        given(mockAuthentication.getPrincipal()).willReturn(mockUser);
        mockTokenGeneration(mockUser, mockUserDto);

        // WHEN
        AuthResponse response = authService.login(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
        assertThat(response.accessTokenExpiresInMs()).isEqualTo(ACCESS_TOKEN_EXPIRATION_MS);
        assertThat(response.refreshTokenExpiresInMs()).isEqualTo(REFRESH_TOKEN_EXPIRATION_MS);

        //VERIFY
        then(authenticationManager).should().authenticate(tokenCaptor.capture());

        UsernamePasswordAuthenticationToken capturedToken = tokenCaptor.getValue();

        assertThat(capturedToken.getPrincipal()).isEqualTo(TEST_EMAIL);
        assertThat(capturedToken.getCredentials()).isEqualTo(TEST_PASSWORD);
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        // GIVEN
        RefreshTokenRequest request = new RefreshTokenRequest(FAKE_REFRESH_TOKEN);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail(TEST_EMAIL);

        UserDto mockUserDto = UserDto.builder().id(1L).email(TEST_EMAIL).build();

        Claims mockClaims = mock(Claims.class);
        given(jwtService.extractAllClaims(FAKE_REFRESH_TOKEN)).willReturn(mockClaims);
        given(mockClaims.getSubject()).willReturn(TEST_EMAIL);
        given(mockClaims.get("type", String.class)).willReturn("REFRESH");
        given(userService.getByEmail(TEST_EMAIL)).willReturn(mockUser);
        


        mockTokenGeneration(mockUser, mockUserDto);

        // WHEN
        AuthResponse response = authService.refreshToken(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenIsInvalid() {
        // GIVEN
        RefreshTokenRequest request = new RefreshTokenRequest("BROKEN_TOKEN");
        Claims mockBrokenClaims = mock(Claims.class);
        given(jwtService.extractAllClaims("BROKEN_TOKEN")).willReturn(mockBrokenClaims);
        given(mockBrokenClaims.get("type", String.class)).willReturn("REFRESH");
        given(mockBrokenClaims.getSubject()).willReturn(null);

        // WHEN & THEN
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenRequestIsNull() {
        assertThatThrownBy(() -> authService.refreshToken(null))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenIsNull() {
        RefreshTokenRequest request = new RefreshTokenRequest(null);
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenIsBlank() {
        RefreshTokenRequest request = new RefreshTokenRequest("   ");
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void shouldThrowExceptionWhenTokenTypeIsInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest(FAKE_ACCESS_TOKEN);
        Claims mockClaims = mock(Claims.class);
        given(jwtService.extractAllClaims(FAKE_ACCESS_TOKEN)).willReturn(mockClaims);
        given(mockClaims.get("type", String.class)).willReturn("ACCESS");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid token type");
    }

    @Test
    void shouldThrowExceptionWhenJwtParsingFails() {
        RefreshTokenRequest request = new RefreshTokenRequest("MALFORMED_TOKEN");
        given(jwtService.extractAllClaims("MALFORMED_TOKEN")).willThrow(new JwtException("Invalid signature"));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid refresh token");
    }

    private void mockTokenGeneration(User user, UserDto userDto) {
        given(jwtService.generateAccessToken(user)).willReturn(FAKE_ACCESS_TOKEN);
        given(jwtService.generateRefreshToken(user)).willReturn(FAKE_REFRESH_TOKEN);
        given(jwtService.getAccessTokenExpirationMs()).willReturn(ACCESS_TOKEN_EXPIRATION_MS);
        given(jwtService.getRefreshTokenExpirationMs()).willReturn(REFRESH_TOKEN_EXPIRATION_MS);
        given(userMapper.toDto(user)).willReturn(userDto);
    }

}
