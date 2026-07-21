package com.example.fincorelite.shared.api;

import com.example.fincorelite.shared.web.correlation.CorrelationIdAccessor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Objects;

@Component
public class ApiResponseFactory {

    private final Clock clock;
    private final CorrelationIdAccessor correlationIdAccessor;

    public ApiResponseFactory(
            Clock clock,
            CorrelationIdAccessor correlationIdAccessor
    ) {
        this.clock = Objects.requireNonNull(
                clock,
                "clock must not be null"
        );

        this.correlationIdAccessor =
                Objects.requireNonNull(
                        correlationIdAccessor,
                        "correlationIdAccessor must not be null"
                );
    }

    public <T> ApiResponse<T> success(T data) {
        Objects.requireNonNull(
                data,
                "data must not be null"
        );

        ApiMeta meta = new ApiMeta(
                clock.instant(),
                correlationIdAccessor.current()
        );

        return new ApiResponse<>(
                true,
                data,
                meta
        );
    }
}