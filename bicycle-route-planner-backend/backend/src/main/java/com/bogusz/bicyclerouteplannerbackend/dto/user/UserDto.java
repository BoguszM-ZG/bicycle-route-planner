package com.bogusz.bicyclerouteplannerbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class UserDto{
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private Boolean active;
    private Instant createdAt;

}

