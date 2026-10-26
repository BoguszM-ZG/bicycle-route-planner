package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.exception.UserAlreadyExistsException;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(String email, String rawPassword, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User with given email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole("ROLE_USER");
        user.setIsActive(Boolean.TRUE);
        User savedUser = userRepository.save(user);

        log.info("Registered new user with ID: {}", savedUser.getId());

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
