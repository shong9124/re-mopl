package com.practice.remopl.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserCreateRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void passwordMustNotExceed72Utf8Bytes() {
        UserCreateRequest request = new UserCreateRequest(
                "사용자",
                "user@example.com",
                "a1" + "가".repeat(24)
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")
                        && violation.getMessage().contains("72바이트"));
    }

    @Test
    void passwordAcceptsAtMost72Utf8Bytes() {
        UserCreateRequest request = new UserCreateRequest(
                "사용자",
                "user@example.com",
                "a1" + "가".repeat(23)
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nameAndEmailMustRespectDatabaseColumnLengths() {
        UserCreateRequest request = new UserCreateRequest(
                "a".repeat(101),
                "a".repeat(244) + "@example.com",
                "password1"
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "email");
    }
}
