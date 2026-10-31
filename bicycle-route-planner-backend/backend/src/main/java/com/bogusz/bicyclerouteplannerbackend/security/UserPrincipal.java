package com.bogusz.bicyclerouteplannerbackend.security;

import com.bogusz.bicyclerouteplannerbackend.model.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
@EqualsAndHashCode(of = {"id", "email"})
@ToString(exclude = "password")
public class UserPrincipal implements UserDetails, Principal {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final boolean enabled;
    private final String fullName;
    private final Instant createdAt;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String password, String role, boolean enabled, String fullName, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = (role != null && !role.isBlank()) ? role : DEFAULT_ROLE;
        this.enabled = enabled;
        this.fullName = fullName;
        this.createdAt = createdAt;
        this.authorities = List.of(new SimpleGrantedAuthority(this.role));
    }

    public static UserPrincipal fromUser(User user) {
        Objects.requireNonNull(user, "User must not be null");
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isEnabled(),
                user.getFullName(),
                user.getCreatedAt()
        );
    }

    public static UserPrincipal fromJwt(Long id, String email, String role) {
        return new UserPrincipal(id, email, null, role, true, null, null);
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String role() {
        return role;
    }

    @Override
    @NonNull
    public String getName() {
        return getUsername();
    }

    @Override
    @NonNull
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
