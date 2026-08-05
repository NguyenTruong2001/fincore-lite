package com.example.fincorelite.shared.error;

import java.util.Objects;


public record FieldViolation(
        String field,
        String code,
        String message
) {

    public FieldViolation {
        field = requireText(
                field,
                "field"
        );

        code = requireText(
                code,
                "code"
        );

        message = requireText(
                message,
                "message"
        );
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value;
    }
}
