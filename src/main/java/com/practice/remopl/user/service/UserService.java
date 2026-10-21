package com.practice.remopl.user.service;

import com.practice.remopl.auth.support.EmailNormalizer;
import com.practice.remopl.user.dto.request.UserCreateRequest;
import com.practice.remopl.user.dto.response.UserResponse;
import com.practice.remopl.user.entity.User;
import com.practice.remopl.user.exception.DuplicateEmailException;
import com.practice.remopl.user.mapper.UserMapper;
import com.practice.remopl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest dto) {
        String normalizedEmail = EmailNormalizer.normalize(dto.email());

        validateDuplicateEmail(normalizedEmail);

        String encodedPassword = passwordEncoder.encode(dto.password());
        User user = User.create(normalizedEmail, encodedPassword, dto.name());

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEmailViolation(e)) {
                throw new DuplicateEmailException();
            }
            throw e;
        }

        log.info(
                "User created: 회원이 성공적으로 생성되었습니다. userId={}",
                savedUser.getId()
        );

        return userMapper.toDto(savedUser);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
    }

    private boolean isDuplicateEmailViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                return "uk_users_email".equals(constraintViolationException.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}
