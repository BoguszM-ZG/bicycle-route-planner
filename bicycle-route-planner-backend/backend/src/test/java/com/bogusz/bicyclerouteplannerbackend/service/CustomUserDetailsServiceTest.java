package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    private static final Long USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String PASSWORD = "hashedPassword";
    private static final String ROLE_USER = "ROLE_USER";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldLoadUserByUsernameSuccessfully() {
        // GIVEN
        User mockUser = new User();
        mockUser.setId(USER_ID);
        mockUser.setEmail(TEST_EMAIL);
        mockUser.setPassword(PASSWORD);
        mockUser.setRole(ROLE_USER);
        mockUser.setIsActive(true);

        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(mockUser));

        // WHEN
        UserDetails result = userDetailsService.loadUserByUsername(TEST_EMAIL);

        // THEN
        assertThat(result)
                .isNotNull()
                .isInstanceOf(UserPrincipal.class);

        UserPrincipal principal = (UserPrincipal) result;
        assertThat(principal.getId()).isEqualTo(USER_ID);
        assertThat(principal.getUsername()).isEqualTo(TEST_EMAIL);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // GIVEN
        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(TEST_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
