package com.bogusz.bicyclerouteplannerbackend.config;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            Claims claims = jwtService.extractAllClaims(jwt);
            
            if (!"ACCESS".equals(claims.get("type", String.class))) {
                throw new JwtException("Invalid token type");
            }

            final String userEmail = claims.getSubject();
            final Long userId = claims.get("userId", Long.class);
            final String userRole = claims.get("role", String.class);

            if (userEmail != null && userId != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UsernamePasswordAuthenticationToken authToken = createAuthToken(userId, userEmail, userRole, request);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Error validating JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken createAuthToken(Long userId, String userEmail,
            String userRole, HttpServletRequest request) {

        UserPrincipal principal = new UserPrincipal(userId, userEmail);
        String role = (userRole != null) ? userRole : "ROLE_USER";

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        return authToken;
    }
}