package com.example.fincorelite.shared.api;

import java.time.Instant;
import java.util.Objects;

public record ApiMeta(
        Instant timestamp) {

    public ApiMeta {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
