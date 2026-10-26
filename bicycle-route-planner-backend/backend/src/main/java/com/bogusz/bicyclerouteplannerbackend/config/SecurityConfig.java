package com.bogusz.bicyclerouteplannerbackend.config;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200"
    );
    private static final List<String> ALLOWED_HTTP_METHODS = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    );

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/telemetry/**",
                                "/api/routing/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Unauthorized access attempt to {}: {}", request.getRequestURI(), authException.getMessage());
                            if (response.isCommitted()) {
                                return;
                            }
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                                    HttpStatus.UNAUTHORIZED,
                                    "Full authentication is required to access this resource"
                            );
                            problemDetail.setTitle("Unauthorized");
                            problemDetail.setType(URI.create("urn:problem-type:unauthorized"));
                            problemDetail.setProperty("timestamp", Instant.now());
                            objectMapper.writeValue(response.getOutputStream(), problemDetail);
                            response.getOutputStream().flush();
                        })
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = new ArrayList<>(DEFAULT_ALLOWED_ORIGINS);
        if (frontendUrl != null && !frontendUrl.isBlank() && !allowedOrigins.contains(frontendUrl)) {
            allowedOrigins.add(frontendUrl);
        }
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(ALLOWED_HTTP_METHODS);
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}