package com.example.fincorelite.shared.api;

import java.time.Instant;
import java.util.Objects;

public record ApiMeta(
        Instant timestamp,
        String correlationId
) {

    public ApiMeta {
        Objects.requireNonNull(
                timestamp,
                "timestamp must not be null"
        );

        Objects.requireNonNull(
                correlationId,
                "correlationId must not be null"
        );

        if (correlationId.isBlank()) {
            throw new IllegalArgumentException(
                    "correlationId must not be blank"
            );
        }
    }
}
