package com.practice.remopl.user.dto.request;

import com.practice.remopl.user.validation.BcryptPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(max = 100, message = "사용자명은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @BcryptPassword
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,}$",
                message = "비밀번호는 영문자와 숫자를 포함하여 8자 이상이어야 합니다."
        )
        String password

) { }
