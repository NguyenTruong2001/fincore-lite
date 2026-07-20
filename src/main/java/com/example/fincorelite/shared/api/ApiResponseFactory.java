package com.example.fincorelite.shared.api;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class ApiResponseFactory {
    private final Clock clock;

    public ApiResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public <T> ApiResponse<T> success(T data) {
        ApiMeta meta = new ApiMeta(Instant.now(clock));
        return new ApiResponse<>(true, data, meta);
    }
}
