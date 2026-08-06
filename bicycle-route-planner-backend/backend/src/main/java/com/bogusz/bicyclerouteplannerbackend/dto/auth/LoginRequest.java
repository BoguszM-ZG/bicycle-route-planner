package com.bogusz.bicyclerouteplannerbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class LoginRequest{
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;

}
