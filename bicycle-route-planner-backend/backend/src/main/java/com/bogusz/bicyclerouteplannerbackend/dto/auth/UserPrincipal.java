package com.bogusz.bicyclerouteplannerbackend.dto.auth;

import java.security.Principal;

public record UserPrincipal(Long id, String email) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}
