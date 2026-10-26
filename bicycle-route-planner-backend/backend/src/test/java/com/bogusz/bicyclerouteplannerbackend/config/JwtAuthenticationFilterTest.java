package com.bogusz.bicyclerouteplannerbackend.config;

import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldDoFilterWithoutSettingAuthenticationWhenAuthHeaderIsMissing() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    void shouldDoFilterWithoutSettingAuthenticationWhenHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    void shouldAuthenticateUserAndSetAuthoritiesFromPrincipalWhenTokenIsValid() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserPrincipal principal = UserPrincipal.of(42L, "test@example.com", "ROLE_USER");

        given(jwtService.parseAccessToken(VALID_TOKEN)).willReturn(Optional.of(principal));

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(principal);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void shouldClearSecurityContextWhenTokenIsInvalid() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.parseAccessToken("invalid.jwt.token")).willReturn(Optional.empty());

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void shouldNotParseTokenWhenAuthenticationIsAlreadyPresentInContext() throws ServletException, IOException {
        // Given
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuth);
        then(jwtService).shouldHaveNoInteractions();
        then(filterChain).should().doFilter(request, response);
    }
}
