package com.bogusz.bicyclerouteplannerbackend.mapper;

import com.bogusz.bicyclerouteplannerbackend.dto.user.UserDto;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
