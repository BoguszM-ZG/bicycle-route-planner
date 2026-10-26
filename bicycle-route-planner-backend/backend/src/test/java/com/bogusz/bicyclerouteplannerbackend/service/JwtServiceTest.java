package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.config.JwtConfig;
import com.bogusz.bicyclerouteplannerbackend.exception.RefreshTokenException;
import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String VALID_SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposes123!";
    private static final long EXPIRATION_MS = 3600000L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 55L;
    private static final String TEST_ROLE = "ROLE_USER";

    @Mock
    private JwtConfig jwtConfig;

    private JwtService jwtService;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testPrincipal = UserPrincipal.of(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

        given(jwtConfig.getSecret()).willReturn(VALID_SECRET);
        jwtService = new JwtService(jwtConfig);
    }

    @Test
    void shouldGenerateAndExtractAccessTokenSuccessfully() {
        // GIVEN
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        String token = jwtService.generateAccessToken(testPrincipal);

        // THEN
        assertThat(token).isNotBlank();

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(TEST_EMAIL);
        assertThat(claims.get("userId", Long.class)).isEqualTo(TEST_USER_ID);
        assertThat(claims.get("role", String.class)).isEqualTo(TEST_ROLE);
        assertThat(claims.get("type", String.class)).isEqualTo("ACCESS");
    }

    @Test
    void shouldParseAccessTokenSuccessfully() {
        // GIVEN
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        String token = jwtService.generateAccessToken(testPrincipal);
        Optional<UserPrincipal> principalOpt = jwtService.parseAccessToken(token);

        // THEN
        assertThat(principalOpt).isPresent();
        UserPrincipal principal = principalOpt.get();
        assertThat(principal.id()).isEqualTo(TEST_USER_ID);
        assertThat(principal.email()).isEqualTo(TEST_EMAIL);
        assertThat(principal.role()).isEqualTo(TEST_ROLE);
    }

    @Test
    void shouldReturnEmptyWhenParsingNonAccessToken() {
        // GIVEN
        given(jwtConfig.getRefreshTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        String refreshToken = jwtService.generateRefreshToken(testPrincipal);
        Optional<UserPrincipal> principalOpt = jwtService.parseAccessToken(refreshToken);

        // THEN
        assertThat(principalOpt).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenParsingMalformedAccessToken() {
        // GIVEN
        String malformedToken = "malformed.token.value";

        // WHEN
        Optional<UserPrincipal> principalOpt = jwtService.parseAccessToken(malformedToken);

        // THEN
        assertThat(principalOpt).isEmpty();
    }

    @Test
    void shouldGenerateAndExtractRefreshTokenSuccessfully() {
        // GIVEN
        given(jwtConfig.getRefreshTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        String token = jwtService.generateRefreshToken(testPrincipal);

        // THEN
        assertThat(token).isNotBlank();

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(TEST_EMAIL);
        assertThat(claims.get("userId", Long.class)).isEqualTo(TEST_USER_ID);
        assertThat(claims.get("role", String.class)).isNull();
        assertThat(claims.get("type", String.class)).isEqualTo("REFRESH");
    }

    @Test
    void shouldThrowExceptionWhenSecretIsTooShort() {
        // GIVEN
        JwtConfig shortSecretConfig = mock(JwtConfig.class);
        given(shortSecretConfig.getSecret()).willReturn("TooShort");

        // WHEN & THEN
        assertThatThrownBy(() -> new JwtService(shortSecretConfig))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be at least 32 characters long");
    }

    @Test
    void shouldThrowExceptionWhenExtractingFromExpiredToken() {
        // GIVEN
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(-1000L);
        String expiredToken = jwtService.generateAccessToken(testPrincipal);

        // WHEN & THEN
        assertThatThrownBy(() -> jwtService.extractAllClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldThrowExceptionWhenExtractingFromManipulatedToken() {
        // GIVEN
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(EXPIRATION_MS);

        String validToken = jwtService.generateAccessToken(testPrincipal);
        String manipulatedToken = validToken.substring(0, validToken.length() - 5) + "HACK";

        // WHEN & THEN
        assertThatThrownBy(() -> jwtService.extractAllClaims(manipulatedToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void shouldExtractEmailFromValidRefreshToken() {
        // GIVEN
        given(jwtConfig.getRefreshTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        String refreshToken = jwtService.generateRefreshToken(testPrincipal);
        String email = jwtService.extractEmailFromRefreshToken(refreshToken);

        // THEN
        assertThat(email).isEqualTo(TEST_EMAIL);
    }

    @Test
    void shouldThrowExceptionWhenExtractingEmailFromAccessTokenInsteadOfRefreshToken() {
        // GIVEN
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(EXPIRATION_MS);
        String accessToken = jwtService.generateAccessToken(testPrincipal);

        // WHEN & THEN
        assertThatThrownBy(() -> jwtService.extractEmailFromRefreshToken(accessToken))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid token type");
    }

    @Test
    void shouldThrowExceptionWhenExtractingEmailFromMalformedRefreshToken() {
        // GIVEN
        String invalidRefreshToken = "INVALID.MALFORMED.TOKEN";

        // WHEN & THEN
        assertThatThrownBy(() -> jwtService.extractEmailFromRefreshToken(invalidRefreshToken))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void shouldReturnAccessTokenExpirationMs() {
        // GIVEN
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        long result = jwtService.getAccessTokenExpirationMs();

        // THEN
        assertThat(result).isEqualTo(EXPIRATION_MS);
    }

    @Test
    void shouldReturnRefreshTokenExpirationMs() {
        // GIVEN
        given(jwtConfig.getRefreshTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        long result = jwtService.getRefreshTokenExpirationMs();

        // THEN
        assertThat(result).isEqualTo(EXPIRATION_MS);
    }
}
