package com.example.fincorelite.shared.error;

import java.util.Objects;

public class BusinessException
        extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(
            ErrorCode errorCode,
            String detail
    ) {
        super(requireDetail(detail));

        this.errorCode = Objects.requireNonNull(
                errorCode,
                "errorCode must not be null"
        );
    }

    public BusinessException(
            ErrorCode errorCode,
            String detail,
            Throwable cause
    ) {
        super(
                requireDetail(detail),
                cause
        );

        this.errorCode = Objects.requireNonNull(
                errorCode,
                "errorCode must not be null"
        );
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static String requireDetail(
            String detail
    ) {
        Objects.requireNonNull(
                detail,
                "detail must not be null"
        );

        if (detail.isBlank()) {
            throw new IllegalArgumentException(
                    "detail must not be blank"
            );
        }

        return detail;
    }
}
