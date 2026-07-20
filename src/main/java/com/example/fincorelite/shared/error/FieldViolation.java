package com.example.fincorelite.shared.error;

import java.util.Objects;

public record FieldViolation(String field, String message) {
    public FieldViolation {
        field = requireNonBlank(field, "field must not be blank");
        message = requireNonBlank(message, "message must not be blank");
    }

    private static String requireNonBlank(String value,
                                          String errorMessage) {
        Objects.requireNonNull(value, errorMessage);
        if (value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }
}
