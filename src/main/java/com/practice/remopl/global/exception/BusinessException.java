package com.practice.remopl.global.exception;

import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String exceptionType;
    private final String message; // client 에게 보낼 message
    private final String detailMessage; // logging 을 위한 message

    public BusinessException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public BusinessException(HttpStatus status, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.exceptionType = this.getClass().getSimpleName();
        this.message = message;
        this.detailMessage = formatDetailMessage(this.message, details);
    }

    private String formatDetailMessage(String defaultMessage, Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return defaultMessage;
        }
        return details.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue().toString())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.exceptionType + "[", "]")
                .add("HttpStatus=" + status)
                .add("detailMessage='" + detailMessage + "'")
                .toString();
    }
}

