package com.practice.remopl.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.practice.remopl.user.dto.request.UserCreateRequest;
import com.practice.remopl.user.entity.User;
import com.practice.remopl.user.exception.DuplicateEmailException;
import com.practice.remopl.user.mapper.UserMapper;
import com.practice.remopl.user.repository.UserRepository;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService userService = new UserService(
            userMapper,
            userRepository,
            passwordEncoder
    );

    @Test
    void convertsEmailUniqueConstraintViolationToDuplicateEmailException() {
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(integrityViolation("uk_users_email"));

        UserCreateRequest request = new UserCreateRequest(
                "사용자",
                "user@example.com",
                "password1"
        );

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void rethrowsOtherIntegrityViolations() {
        DataIntegrityViolationException exception = integrityViolation("other_constraint");
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(exception);

        UserCreateRequest request = new UserCreateRequest(
                "사용자",
                "user@example.com",
                "password1"
        );

        assertThatThrownBy(() -> userService.create(request)).isSameAs(exception);
    }

    private DataIntegrityViolationException integrityViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "constraint violation",
                new SQLException(),
                constraintName
        );
        return new DataIntegrityViolationException("data integrity violation", cause);
    }
}
