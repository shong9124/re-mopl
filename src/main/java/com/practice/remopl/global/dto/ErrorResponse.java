package com.practice.remopl.global.dto;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.practice.remopl.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

public record ErrorResponse(String exceptionType, String message, Object details) {

    public static ErrorResponse from(BusinessException ex) {
        return new ErrorResponse(ex.getExceptionType(), ex.getMessage(), null);
    }

    public static ErrorResponse from(MethodArgumentNotValidException ex) {
        Map<String, List<String>> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(
                                error -> error.getDefaultMessage() == null ? "유효하지 않은 입력값입니다."
                                        : error.getDefaultMessage(),
                                Collectors.toList()
                        )
                ));
        return new ErrorResponse("INVALID_INPUT", "잘못된 입력값입니다.", details);
    }

    public static ErrorResponse from(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            List<JsonMappingException.Reference> path = invalidFormatException.getPath();

            String fieldName = "request";

            // 배열 인덱스 등 fieldName이 null인 경우를 고려해 뒤에서부터 실제 필드명을 찾는다.
            for (int i = path.size() - 1; i >= 0; i--) {
                String candidate = path.get(i).getFieldName();
                if (candidate != null) {
                    fieldName = candidate;
                    break;
                }
            }

            Object invalidValue = invalidFormatException.getValue();

            return new ErrorResponse(
                    "INVALID_INPUT",
                    "잘못된 입력값입니다.",
                    Map.of(
                            fieldName,
                            List.of("허용되지 않는 값입니다: " + invalidValue)
                    )
            );
        }

        return new ErrorResponse(
                "INVALID_INPUT",
                "요청 본문을 읽을 수 없습니다.",
                null
        );
    }

    public static ErrorResponse from(MissingServletRequestParameterException ex) {
        Map<String, List<String>> details = Map.of(
                ex.getParameterName(),
                List.of("필수 파라미터 '" + ex.getParameterName() + "'이(가) 누락되었습니다.")
        );
        return new ErrorResponse("INVALID_INPUT", "잘못된 입력값입니다.", details);
    }

    public static ErrorResponse from(MethodArgumentTypeMismatchException ex) {
        Map<String, List<String>> details = Map.of(
                ex.getName(),
                List.of("허용되지 않는 값입니다: " + ex.getValue())
        );
        return new ErrorResponse("INVALID_INPUT", "잘못된 입력값입니다.", details);
    }

    public static ErrorResponse from(
            AuthenticationException exception
    ) {
        boolean locked = exception instanceof LockedException;

        return new ErrorResponse(
                locked ? "ACCOUNT_LOCKED" : "INVALID_CREDENTIALS",
                locked ? "잠긴 계정입니다." : "이메일 또는 비밀번호가 올바르지 않습니다.",
                locked
                        ? null
                        : Map.of("loginFailed", List.of("Invalid credentials"))
        );
    }

    public static ErrorResponse from(DataIntegrityViolationException ex) {
        return new ErrorResponse(
                "CONFLICT",
                "이미 존재하거나 사용할 수 없는 데이터입니다.",
                null
        );
    }
}
