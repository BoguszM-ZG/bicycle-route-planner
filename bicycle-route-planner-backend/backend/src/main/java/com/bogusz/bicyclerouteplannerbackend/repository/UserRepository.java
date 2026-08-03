package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}