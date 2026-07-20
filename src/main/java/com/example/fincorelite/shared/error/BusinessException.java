package com.example.fincorelite.shared.error;

import java.util.Objects;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(validateDetail(detail), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static String validateDetail(String detail) {
        Objects.requireNonNull(detail, "detail must not be null");
        if (detail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        return detail;
    }

}
