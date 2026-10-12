package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.UserAlreadyExistsException;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "encodedPassword123";
    private static final String TEST_FULL_NAME = "Test User";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        given(userRepository.existsByEmail(TEST_EMAIL)).willReturn(false);

        User mockSavedUser = new User();
        mockSavedUser.setId(TEST_USER_ID);
        mockSavedUser.setEmail(TEST_EMAIL);
        mockSavedUser.setRole("ROLE_USER");
        mockSavedUser.setIsActive(true);

        given(userRepository.save(any(User.class))).willReturn(mockSavedUser);

        // When
        User result = userService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_USER_ID);

        // Verify
        then(userRepository).should().save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(capturedUser.getPassword()).isEqualTo(TEST_PASSWORD);
        assertThat(capturedUser.getFullName()).isEqualTo(TEST_FULL_NAME);
        assertThat(capturedUser.getRole()).isEqualTo("ROLE_USER");
        assertThat(capturedUser.getIsActive()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenRegisteringWithExistingEmail() {
        // Given
        given(userRepository.existsByEmail(TEST_EMAIL)).willReturn(true);

        // When & THEN
        assertThatThrownBy(() -> userService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        // VERIFY
        then(userRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldGetUserByEmailSuccessfully() {
        // Given
        User mockUser = new User();
        mockUser.setId(TEST_USER_ID);
        mockUser.setEmail(TEST_EMAIL);

        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(mockUser));

        // When
        User result = userService.getByEmail(TEST_EMAIL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    void shouldThrowExceptionWhenGettingByNonExistentEmail() {
        // Given
        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

        // When & THEN
        assertThatThrownBy(() -> userService.getByEmail(TEST_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldLoadUserByUsernameSuccessfully() {
        // Given
        User mockUser = new User();
        mockUser.setEmail(TEST_EMAIL);


        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(mockUser));

        // When
        UserDetails result = userService.loadUserByUsername(TEST_EMAIL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(TEST_EMAIL);
    }
}