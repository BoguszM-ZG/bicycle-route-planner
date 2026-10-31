package com.bogusz.bicyclerouteplannerbackend.dto.user;

import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;

import java.time.Instant;

public record UserDto(
        Long id,
        String email,
        String fullName,
        String role,
        Boolean active,
        Instant createdAt
) {
    public static UserDto from(UserPrincipal principal) {
        return new UserDto(
                principal.getId(),
                principal.getEmail(),
                principal.getFullName(),
                principal.getRole(),
                principal.isEnabled(),
                principal.getCreatedAt()
        );
    }
}