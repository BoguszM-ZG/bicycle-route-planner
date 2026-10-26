package com.bogusz.bicyclerouteplannerbackend.security;

import com.bogusz.bicyclerouteplannerbackend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPrincipalTest {

    private static final Long USER_ID = 10L;
    private static final Long JWT_USER_ID = 25L;
    private static final Long DEFAULT_USER_ID = 1L;
    private static final Long ANOTHER_USER_ID = 999L;
    private static final String USER_EMAIL = "user@example.com";
    private static final String JWT_EMAIL = "jwt@example.com";
    private static final String DEFAULT_EMAIL = "test@test.com";
    private static final String ANOTHER_EMAIL = "another@example.com";
    private static final String PASSWORD_HASH = "hashedPass";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String BLANK_ROLE = "   ";
    private static final String FULL_NAME = "Jan Kowalski";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final String NULL_USER_ERROR_MESSAGE = "User must not be null";

    @Test
    void shouldCreateFromCanonicalConstructorCorrectly() {
        // WHEN
        UserPrincipal principal = new UserPrincipal(USER_ID, USER_EMAIL, PASSWORD_HASH, ROLE_ADMIN, true, FULL_NAME, CREATED_AT);

        // THEN
        assertThat(principal.getId()).isEqualTo(USER_ID);
        assertThat(principal.id()).isEqualTo(USER_ID);
        assertThat(principal.getEmail()).isEqualTo(USER_EMAIL);
        assertThat(principal.email()).isEqualTo(USER_EMAIL);
        assertThat(principal.getUsername()).isEqualTo(USER_EMAIL);
        assertThat(principal.getName()).isEqualTo(USER_EMAIL);
        assertThat(principal.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(principal.getRole()).isEqualTo(ROLE_ADMIN);
        assertThat(principal.role()).isEqualTo(ROLE_ADMIN);
        assertThat(principal.getFullName()).isEqualTo(FULL_NAME);
        assertThat(principal.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_ADMIN);
    }

    @Test
    void shouldCreateFromUserCorrectly() {
        // GIVEN
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setPassword(PASSWORD_HASH);
        user.setRole(ROLE_USER);
        user.setIsActive(true);
        user.setFullName(FULL_NAME);
        user.setCreatedAt(CREATED_AT);

        // WHEN
        UserPrincipal principal = UserPrincipal.fromUser(user);

        // THEN
        assertThat(principal.getId()).isEqualTo(USER_ID);
        assertThat(principal.id()).isEqualTo(USER_ID);
        assertThat(principal.getEmail()).isEqualTo(USER_EMAIL);
        assertThat(principal.email()).isEqualTo(USER_EMAIL);
        assertThat(principal.getUsername()).isEqualTo(USER_EMAIL);
        assertThat(principal.getName()).isEqualTo(USER_EMAIL);
        assertThat(principal.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.role()).isEqualTo(ROLE_USER);
        assertThat(principal.getFullName()).isEqualTo(FULL_NAME);
        assertThat(principal.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldCreateFromJwtCorrectly() {
        // WHEN
        UserPrincipal principal = UserPrincipal.fromJwt(JWT_USER_ID, JWT_EMAIL, ROLE_ADMIN);

        // THEN
        assertThat(principal.getId()).isEqualTo(JWT_USER_ID);
        assertThat(principal.getEmail()).isEqualTo(JWT_EMAIL);
        assertThat(principal.getRole()).isEqualTo(ROLE_ADMIN);
        assertThat(principal.getPassword()).isNull();
        assertThat(principal.getFullName()).isNull();
        assertThat(principal.getCreatedAt()).isNull();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_ADMIN);
    }

    @Test
    void shouldCreateWithFactoryOfWithIdAndEmail() {
        // WHEN
        UserPrincipal principal = UserPrincipal.of(USER_ID, USER_EMAIL);

        // THEN
        assertThat(principal.getId()).isEqualTo(USER_ID);
        assertThat(principal.getEmail()).isEqualTo(USER_EMAIL);
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.getPassword()).isNull();
        assertThat(principal.getFullName()).isNull();
        assertThat(principal.getCreatedAt()).isNull();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldCreateWithFactoryOfWithIdEmailAndRole() {
        // WHEN
        UserPrincipal principal = UserPrincipal.of(USER_ID, USER_EMAIL, ROLE_ADMIN);

        // THEN
        assertThat(principal.getId()).isEqualTo(USER_ID);
        assertThat(principal.getEmail()).isEqualTo(USER_EMAIL);
        assertThat(principal.getRole()).isEqualTo(ROLE_ADMIN);
        assertThat(principal.getPassword()).isNull();
        assertThat(principal.getFullName()).isNull();
        assertThat(principal.getCreatedAt()).isNull();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_ADMIN);
    }

    @Test
    void shouldDefaultToRoleUserWhenRoleIsNullInCanonicalConstructor() {
        // WHEN
        UserPrincipal principal = new UserPrincipal(DEFAULT_USER_ID, DEFAULT_EMAIL, PASSWORD_HASH, null, true, FULL_NAME, CREATED_AT);

        // THEN
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldDefaultToRoleUserWhenRoleIsNullInOfFactory() {
        // WHEN
        UserPrincipal principal = UserPrincipal.of(DEFAULT_USER_ID, DEFAULT_EMAIL, null);

        // THEN
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldDefaultToRoleUserWhenRoleIsNullInFromJwt() {
        // WHEN
        UserPrincipal principal = UserPrincipal.fromJwt(DEFAULT_USER_ID, DEFAULT_EMAIL, null);

        // THEN
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldDefaultToRoleUserWhenRoleIsBlankInCanonicalConstructor() {
        // WHEN
        UserPrincipal principal = new UserPrincipal(DEFAULT_USER_ID, DEFAULT_EMAIL, PASSWORD_HASH, BLANK_ROLE, true, FULL_NAME, CREATED_AT);

        // THEN
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldDefaultToRoleUserWhenRoleIsBlankInOfFactory() {
        // WHEN
        UserPrincipal principal = UserPrincipal.of(DEFAULT_USER_ID, DEFAULT_EMAIL, BLANK_ROLE);

        // THEN
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldDefaultToRoleUserWhenRoleIsBlankInFromJwt() {
        // WHEN
        UserPrincipal principal = UserPrincipal.fromJwt(DEFAULT_USER_ID, DEFAULT_EMAIL, BLANK_ROLE);

        // THEN
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldCreateFromJwtWithDefaultRoleWhenRoleNotProvided() {
        // WHEN
        UserPrincipal principal = UserPrincipal.fromJwt(JWT_USER_ID, JWT_EMAIL);

        // THEN
        assertThat(principal.getId()).isEqualTo(JWT_USER_ID);
        assertThat(principal.getEmail()).isEqualTo(JWT_EMAIL);
        assertThat(principal.getRole()).isEqualTo(ROLE_USER);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_USER);
    }

    @Test
    void shouldCreateDisabledUserWhenUserIsInactive() {
        // GIVEN
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setPassword(PASSWORD_HASH);
        user.setRole(ROLE_USER);
        user.setIsActive(false);

        // WHEN
        UserPrincipal principal = UserPrincipal.fromUser(user);

        // THEN
        assertThat(principal.isEnabled()).isFalse();
    }

    @Test
    void shouldCreateDisabledUserWhenUserIsActiveIsNull() {
        // GIVEN
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setPassword(PASSWORD_HASH);
        user.setRole(ROLE_USER);
        user.setIsActive(null);

        // WHEN
        UserPrincipal principal = UserPrincipal.fromUser(user);

        // THEN
        assertThat(principal.isEnabled()).isFalse();
    }

    @Test
    void shouldThrowNullPointerExceptionWhenUserIsNull() {
        // WHEN & THEN
        assertThatThrownBy(() -> UserPrincipal.fromUser(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(NULL_USER_ERROR_MESSAGE);
    }

    @Test
    void shouldBeEqualAndHaveSameHashCodeWhenSameIdAndEmail() {
        // GIVEN
        UserPrincipal principal1 = UserPrincipal.of(USER_ID, USER_EMAIL, ROLE_ADMIN);
        UserPrincipal principal2 = UserPrincipal.of(USER_ID, USER_EMAIL, ROLE_USER);

        // WHEN & THEN
        assertThat(principal1).isEqualTo(principal2);
        assertThat(principal1).hasSameHashCodeAs(principal2);
    }

    @Test
    void shouldNotBeEqualWhenDifferentId() {
        // GIVEN
        UserPrincipal principal1 = UserPrincipal.of(USER_ID, USER_EMAIL);
        UserPrincipal principal2 = UserPrincipal.of(ANOTHER_USER_ID, USER_EMAIL);

        // WHEN & THEN
        assertThat(principal1).isNotEqualTo(principal2);
    }

    @Test
    void shouldNotBeEqualWhenDifferentEmail() {
        // GIVEN
        UserPrincipal principal1 = UserPrincipal.of(USER_ID, USER_EMAIL);
        UserPrincipal principal2 = UserPrincipal.of(USER_ID, ANOTHER_EMAIL);

        // WHEN & THEN
        assertThat(principal1).isNotEqualTo(principal2);
    }

    @Test
    void shouldExcludePasswordInToString() {
        // GIVEN
        UserPrincipal principal = new UserPrincipal(USER_ID, USER_EMAIL, PASSWORD_HASH, ROLE_USER, true, FULL_NAME, CREATED_AT);

        // WHEN
        String result = principal.toString();

        // THEN
        assertThat(result)
                .contains(USER_ID.toString())
                .contains(USER_EMAIL)
                .contains(ROLE_USER)
                .doesNotContain(PASSWORD_HASH);
    }

    @Test
    void shouldReturnSameValueForGetNameAndGetUsername() {
        // GIVEN
        UserPrincipal principal = UserPrincipal.of(USER_ID, USER_EMAIL);

        // WHEN & THEN
        assertThat(principal.getName()).isEqualTo(principal.getUsername()).isEqualTo(USER_EMAIL);
    }
}
