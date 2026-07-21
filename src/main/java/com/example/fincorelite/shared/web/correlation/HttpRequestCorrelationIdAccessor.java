package com.example.fincorelite.shared.web.correlation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class HttpRequestCorrelationIdAccessor implements CorrelationIdAccessor {

    private final HttpServletRequest request;

    public HttpRequestCorrelationIdAccessor(HttpServletRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    @Override
    public String current() {
        Object value = request.getAttribute(CorrelationIdConstants.REQUEST_ATTRIBUTE);
        if (!(value instanceof String correlationId) || correlationId.isBlank()) {
            throw new IllegalStateException("Correlation ID not available in the current request");
        }
        return correlationId;
    }
}
