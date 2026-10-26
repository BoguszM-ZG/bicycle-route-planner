package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @NonNull
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(UserPrincipal::fromUser)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email"));
    }
}
