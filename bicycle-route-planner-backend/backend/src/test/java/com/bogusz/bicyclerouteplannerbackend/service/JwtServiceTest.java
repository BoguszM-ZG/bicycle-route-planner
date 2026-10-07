package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.config.JwtConfig;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    // Secret must be at least 32 characters for HS256 algorithm
    private static final String VALID_SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposes123!";
    private static final String INVALID_SECRET = "TooShort";
    private static final long EXPIRATION_MS = 3600000L; // 1 Hour

    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 55L;
    private static final String TEST_ROLE = "ROLE_USER";

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setRole(TEST_ROLE);
    }


    @Test
    void shouldGenerateAndExtractAccessTokenSuccessfully() {
        // GIVEN
        given(jwtConfig.getSecret()).willReturn(VALID_SECRET);
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        String token = jwtService.generateAccessToken(testUser);

        // THEN
        assertThat(token).isNotBlank();

        // Verify
        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(TEST_EMAIL);
        assertThat(claims.get("userId", Long.class)).isEqualTo(TEST_USER_ID);
        assertThat(claims.get("role", String.class)).isEqualTo(TEST_ROLE);
        assertThat(claims.get("type", String.class)).isEqualTo("ACCESS");
    }

    @Test
    void shouldGenerateAndExtractRefreshTokenSuccessfully() {
        // GIVEN
        given(jwtConfig.getSecret()).willReturn(VALID_SECRET);
        given(jwtConfig.getRefreshTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN
        String token = jwtService.generateRefreshToken(testUser);

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
        given(jwtConfig.getSecret()).willReturn(INVALID_SECRET);
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(EXPIRATION_MS);

        // WHEN & THEN
        assertThatThrownBy(() -> jwtService.generateAccessToken(testUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be at least 32 characters long");
    }

    @Test
    void shouldThrowExceptionWhenExtractingFromExpiredToken() {
        // GIVEN
        given(jwtConfig.getSecret()).willReturn(VALID_SECRET);

        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(-1000L);
        String expiredToken = jwtService.generateAccessToken(testUser);

        // WHEN & THEN
        assertThatThrownBy(() -> jwtService.extractAllClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldThrowExceptionWhenExtractingFromManipulatedToken() {
        // GIVEN
        given(jwtConfig.getSecret()).willReturn(VALID_SECRET);
        given(jwtConfig.getAccessTokenExpirationMs()).willReturn(EXPIRATION_MS);

        String validToken = jwtService.generateAccessToken(testUser);

        String manipulatedToken = validToken.substring(0, validToken.length() - 5) + "HACK";

        // WHEN & THEN
        assertThatThrownBy(() -> jwtService.extractAllClaims(manipulatedToken))
                .isInstanceOf(SignatureException.class);
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