package com.bogusz.bicyclerouteplannerbackend.dto.user;

import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UserDto(
        Long id,
        String email,
        String fullName,
        String role,
        Boolean active,
        Instant createdAt
) {
    public static UserDto from(UserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        return new UserDto(
                principal.getId(),
                principal.getEmail(),
                principal.getFullName(),
                principal.getRole(),
                principal.isEnabled(),
                principal.getCreatedAt()
        );
    }

    public static UserDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}