package com.practice.remopl.user.exception;

import com.practice.remopl.global.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class UserException extends BusinessException {

    public UserException(HttpStatus status, String message) {
        super(status, message);
    }

    public UserException(HttpStatus status, String message, Map<String, Object> details) {
        super(status, message, details);
    }
}
