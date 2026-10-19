package com.practice.remopl.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserCreateRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,}$",
                message = "비밀번호는 영문자와 숫자를 포함하여 8자 이상이어야 합니다."
        )
        String password

) { }
