package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    public User createRouteUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("route-placeholder");
        user.setRole("ROLE_USER");
        user.setIsActive(Boolean.TRUE);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

}
