package com.example.fincorelite.shared.api;

import java.util.Objects;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiMeta meta
) {

    public ApiResponse {
        if (!success) {
            throw new IllegalArgumentException(
                    "success must be true"
            );
        }

        Objects.requireNonNull(
                data,
                "data must not be null"
        );

        Objects.requireNonNull(
                meta,
                "meta must not be null"
        );
    }
}